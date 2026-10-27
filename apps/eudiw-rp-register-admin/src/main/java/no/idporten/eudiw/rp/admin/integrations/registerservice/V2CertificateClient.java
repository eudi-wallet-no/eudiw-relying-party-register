package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class V2CertificateClient {

    private final RestClient restClient;

    public V2CertificateClient(@Qualifier("v2CertificateRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public RelyingPartyCertificateResource getAccessCertificate(UUID instanceId, UUID certificateId) {
        return getCertificate(instanceId, "access", certificateId);
    }

    public RelyingPartyCertificateResource getIssuerCertificate(UUID instanceId, UUID certificateId) {
        return getCertificate(instanceId, "issuer", certificateId);
    }

    public RelyingPartyCertificateResource requestAccessCertificate(UUID instanceId, RelyingPartyCsrResource request) {
        return requestCertificate(instanceId, "access", request);
    }

    public RelyingPartyCertificateResource requestIssuerCertificate(UUID instanceId, IssuerCsrResource request) {
        return requestCertificate(instanceId, "issuer", request);
    }

    public void revokeAccessCertificate(UUID instanceId, UUID certificateId) {
        revokeCertificate(instanceId, "access", certificateId);
    }

    public void revokeIssuerCertificate(UUID instanceId, UUID certificateId) {
        revokeCertificate(instanceId, "issuer", certificateId);
    }

    private RelyingPartyCertificateResource getCertificate(UUID instanceId, String type, UUID certificateId) {
        String path = certificatePath(instanceId, type) + "/" + certificateId;
        try {
            var response = restClient.get().uri(path).retrieve()
                .onStatus(status -> status.value() == 404, (request, responseBody) -> {
                    throw new NotFoundException("Certificate not found in register service for " + path);
                })
                .toEntity(RelyingPartyCertificateResource.class);
            RelyingPartyCertificateResource certificate = requireCertificate(response, path);
            if (!certificateId.equals(certificate.id())) {
                throw new UnrecognizedErrorResponseException("Register service returned another certificate for " + path);
            }
            return certificate;
        } catch (NotFoundException e) {
            throw certificateNotFound(instanceId, certificateId, e);
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to retrieve " + path, e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read certificate response from register service for " + path, e);
        }
    }

    private RelyingPartyCertificateResource requestCertificate(UUID instanceId, String type, Object request) {
        String path = certificatePath(instanceId, type);
        try {
            var response = restClient.post().uri(path).body(request).retrieve()
                .toEntity(RelyingPartyCertificateResource.class);
            return requireCertificate(response, path);
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to request " + path
                + "; certificate issuance outcome is unknown", e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read issuance response from register service for " + path, e);
        }
    }

    private void revokeCertificate(UUID instanceId, String type, UUID certificateId) {
        RelyingPartyCertificateResource certificate = getCertificate(instanceId, type, certificateId);
        if (certificate.revocationStatus() >= 0) {
            return;
        }

        String path = certificatePath(instanceId, type) + "/" + certificateId + "/revoke";
        try {
            var response = restClient.patch().uri(path).retrieve()
                .onStatus(status -> status.value() == 404, (request, responseBody) -> {
                    throw new NotFoundException("Certificate not found in register service for " + path);
                })
                .toBodilessEntity();
            if (response.getStatusCode().value() != 204) {
                throw new UnrecognizedErrorResponseException("Register service did not confirm revocation for " + path
                    + ": unexpected HTTP status " + response.getStatusCode());
            }
        } catch (NotFoundException e) {
            throw certificateNotFound(instanceId, certificateId, e);
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to revoke " + path
                + "; certificate revocation outcome is unknown", e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read revocation response from register service for " + path, e);
        }
    }

    private RelyingPartyCertificateResource requireCertificate(
        ResponseEntity<RelyingPartyCertificateResource> response, String path) {
        if (response.getStatusCode().value() != 200) {
            throw new UnrecognizedErrorResponseException("Register service returned unexpected HTTP status for " + path
                + ": " + response.getStatusCode());
        }
        RelyingPartyCertificateResource certificate = response.getBody();
        if (certificate == null || certificate.certificate() == null || certificate.id() == null
            || certificate.revocationStatus() == null || certificate.revocationStatus() < -1) {
            throw new UnrecognizedErrorResponseException("Register service returned an incomplete certificate for " + path);
        }
        return certificate;
    }

    private CertificateNotFoundException certificateNotFound(UUID instanceId, UUID certificateId, NotFoundException cause) {
        var exception = new CertificateNotFoundException(cause.getErrorDescription(), instanceId.toString(), certificateId.toString());
        exception.initCause(cause);
        return exception;
    }

    private String certificatePath(UUID instanceId, String type) {
        return "/instances/" + instanceId + "/certificates/" + type;
    }
}

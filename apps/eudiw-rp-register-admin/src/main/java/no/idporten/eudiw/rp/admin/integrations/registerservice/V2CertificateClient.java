package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
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
            return restClient.get().uri(path).retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new CertificateNotFoundException(
                        response.getStatusCode() + " Certificate not found: " + response.getStatusText(),
                        instanceId.toString(), certificateId.toString());
                })
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new BadRequestException(response.getStatusCode() + " Client error: " + response.getStatusText());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ErrorResponseException(response.getStatusCode().toString(), "Server error: " + response.getStatusText());
                })
                .body(RelyingPartyCertificateResource.class);
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to retrieve " + path, e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read certificate response from register service for " + path, e);
        }
    }

    private RelyingPartyCertificateResource requestCertificate(UUID instanceId, String type, Object request) {
        String path = certificatePath(instanceId, type);
        try {
            return restClient.post().uri(path).body(request).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (httpRequest, response) -> {
                    throw new BadRequestException(response.getStatusCode() + " Client error: " + response.getStatusText());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (httpRequest, response) -> {
                    throw new ErrorResponseException(response.getStatusCode().toString(), "Server error: " + response.getStatusText());
                })
                .body(RelyingPartyCertificateResource.class);
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to request " + path
                + "; certificate issuance outcome is unknown", e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read issuance response from register service for " + path, e);
        }
    }

    private void revokeCertificate(UUID instanceId, String type, UUID certificateId) {
        String path = certificatePath(instanceId, type) + "/" + certificateId + "/revoke";
        try {
            restClient.patch().uri(path).retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new CertificateNotFoundException(
                        response.getStatusCode() + " Certificate not found: " + response.getStatusText(),
                        instanceId.toString(), certificateId.toString());
                })
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new BadRequestException(response.getStatusCode() + " Client error: " + response.getStatusText());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ErrorResponseException(response.getStatusCode().toString(), "Server error: " + response.getStatusText());
                })
                .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new AdminServiceException("Unable to contact register service to revoke " + path
                + "; certificate revocation outcome is unknown", e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read revocation response from register service for " + path, e);
        }
    }

    private String certificatePath(UUID instanceId, String type) {
        return "/instances/" + instanceId + "/certificates/" + type;
    }
}

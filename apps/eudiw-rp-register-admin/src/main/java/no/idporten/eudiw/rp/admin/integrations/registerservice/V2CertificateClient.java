package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;
import java.util.function.Supplier;

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

        return callRegister(path, "Unable to contact register service to retrieve " + path, () -> restClient.get()
            .uri(path)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::isSameCodeAs, certificateNotFound(instanceId, certificateId))
            .body(RelyingPartyCertificateResource.class));
    }

    private RelyingPartyCertificateResource requestCertificate(UUID instanceId, String type, Object request) {
        String path = certificatePath(instanceId, type);
        String connectionFailure = "Unable to contact register service to request " + path
            + "; certificate issuance outcome is unknown";

        return callRegister(path, connectionFailure, () -> restClient.post()
            .uri(path)
            .body(request)
            .retrieve()
            .body(RelyingPartyCertificateResource.class));
    }

    private void revokeCertificate(UUID instanceId, String type, UUID certificateId) {
        String path = certificatePath(instanceId, type) + "/" + certificateId + "/revoke";
        String connectionFailure = "Unable to contact register service to revoke " + path
            + "; certificate revocation outcome is unknown";

        callRegister(path, connectionFailure, () -> restClient.patch()
            .uri(path)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::isSameCodeAs, certificateNotFound(instanceId, certificateId))
            .toBodilessEntity());
    }

    // The shared error handler cannot attach the instance and certificate ids the certificate-not-found page shows.
    private RestClient.ResponseSpec.ErrorHandler certificateNotFound(UUID instanceId, UUID certificateId) {
        return (request, response) -> {
            throw new CertificateNotFoundException(
                response.getStatusCode() + " Certificate not found: " + response.getStatusText(),
                instanceId.toString(), certificateId.toString());
        };
    }

    private <T> T callRegister(String path, String connectionFailure, Supplier<T> registerCall) {
        try {
            return registerCall.get();
        } catch (ResourceAccessException e) {
            throw new AdminServiceException(connectionFailure, e);
        } catch (RestClientException e) {
            throw new UnrecognizedErrorResponseException("Unable to read register service response for " + path, e);
        }
    }

    private String certificatePath(UUID instanceId, String type) {
        return "/instances/" + instanceId + "/certificates/" + type;
    }
}

package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

        return restClient.get()
            .uri(path)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::isSameCodeAs, certificateNotFound(instanceId, certificateId))
            .body(RelyingPartyCertificateResource.class);
    }

    private RelyingPartyCertificateResource requestCertificate(UUID instanceId, String type, Object request) {
        String path = certificatePath(instanceId, type);

        return restClient.post()
            .uri(path)
            .body(request)
            .retrieve()
            .body(RelyingPartyCertificateResource.class);
    }

    private void revokeCertificate(UUID instanceId, String type, UUID certificateId) {
        String path = certificatePath(instanceId, type) + "/" + certificateId + "/revoke";

        restClient.patch()
            .uri(path)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::isSameCodeAs, certificateNotFound(instanceId, certificateId))
            .toBodilessEntity();
    }

    // The shared error handler cannot attach the instance and certificate ids the certificate-not-found page shows.
    private RestClient.ResponseSpec.ErrorHandler certificateNotFound(UUID instanceId, UUID certificateId) {
        return (request, response) -> {
            throw new CertificateNotFoundException(
                response.getStatusCode() + " Certificate not found: " + response.getStatusText(),
                instanceId.toString(), certificateId.toString());
        };
    }

    private String certificatePath(UUID instanceId, String type) {
        return "/instances/" + instanceId + "/certificates/" + type;
    }
}

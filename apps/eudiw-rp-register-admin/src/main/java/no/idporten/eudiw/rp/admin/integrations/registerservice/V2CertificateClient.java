package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
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
        return getCertificate(instanceId, CertificateType.ACCESS, certificateId);
    }

    public RelyingPartyCertificateResource getIssuerCertificate(UUID instanceId, UUID certificateId) {
        return getCertificate(instanceId, CertificateType.ISSUER, certificateId);
    }

    public RelyingPartyCertificateResource requestAccessCertificate(UUID instanceId, RelyingPartyCsrResource request) {
        return requestCertificate(instanceId, CertificateType.ACCESS, request);
    }

    public RelyingPartyCertificateResource requestIssuerCertificate(UUID instanceId, IssuerCsrResource request) {
        return requestCertificate(instanceId, CertificateType.ISSUER, request);
    }

    public void revokeAccessCertificate(UUID instanceId, UUID certificateId) {
        revokeCertificate(instanceId, CertificateType.ACCESS, certificateId);
    }

    public void revokeIssuerCertificate(UUID instanceId, UUID certificateId) {
        revokeCertificate(instanceId, CertificateType.ISSUER, certificateId);
    }

    private RelyingPartyCertificateResource getCertificate(UUID instanceId, CertificateType type, UUID certificateId) {
        String path = certificatePath(instanceId, type) + "/" + certificateId;

        var certificate = restClient.get()
            .uri(path)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::isSameCodeAs, certificateNotFound(instanceId, certificateId))
            .body(RelyingPartyCertificateResource.class);

        if (certificate == null) {
            throw new UnrecognizedErrorResponseException(
                "Unable to retrieve " + path + " from register service: response body is empty");
        }
        return certificate;
    }

    private RelyingPartyCertificateResource requestCertificate(UUID instanceId, CertificateType type, Object request) {
        String path = certificatePath(instanceId, type);

        var certificate = restClient.post()
            .uri(path)
            .body(request)
            .retrieve()
            .body(RelyingPartyCertificateResource.class);

        if (certificate == null) {
            throw new UnrecognizedErrorResponseException(
                "Unable to order certificate from " + path + " in register service: response body is empty");
        }
        return certificate;
    }

    private void revokeCertificate(UUID instanceId, CertificateType type, UUID certificateId) {
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

    private String certificatePath(UUID instanceId, CertificateType type) {
        return "/instances/" + instanceId + "/certificates/" + type.pathSegment();
    }
}

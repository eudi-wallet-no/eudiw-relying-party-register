package no.eudiw.rp.register.integrations.certificateservice;

import no.eudiw.rp.register.domain.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.domain.certificates.X509CertificateConverter;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.security.cert.X509Certificate;

@Component
public class CertificateServiceClient {

    private final RestClient caRestClient;

    public CertificateServiceClient(@Qualifier("caRestClient") RestClient caRestClient) {
        this.caRestClient = caRestClient;
    }

    public X509Certificate requestCertificate(
        PKCS10CertificationRequest csr,
        String orgno,
        String legalName,
        String tradeName,
        String caId
    ) {
        String csrPemStr = PKCS10CertificationRequestConverter.convert(csr);
        String certificatePemStr =
            caRestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/{caId}").build(caId))
                .body(RelyingPartyCertificateRequest
                    .builder()
                    .orgno(orgno)
                    .tradeName(tradeName)
                    .legalName(legalName)
                    .csr(csrPemStr)
                    .build())
                .retrieve()
                .toEntity(String.class)
                .getBody();

        if (certificatePemStr == null) {
            throw new RegisterServiceException("Null body in ca-service success response");
        }

        return X509CertificateConverter.convert(certificatePemStr);
    }

    public HttpStatusCode revokeCertificate(String serialNumber, int reason, String caId) {
        return caRestClient.put()
            .uri(uriBuilder -> uriBuilder.path("/{caId}").build(caId))
            .body(RevocationRequest
                .builder()
                .serialNumber(serialNumber)
                .reason(reason)
                .build())
            .retrieve()
            .toEntity(String.class)
            .getStatusCode();
    }
}

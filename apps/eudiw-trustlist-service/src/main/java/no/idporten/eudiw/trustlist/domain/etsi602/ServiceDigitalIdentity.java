package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;

public record ServiceDigitalIdentity(@NotEmpty String cert) {

    public String getValidCertString() {
        String prefix = "-----BEGIN CERTIFICATE-----";
        String suffix = "-----END CERTIFICATE-----";
        if (!cert.startsWith(prefix) || !cert.endsWith(suffix)) {
            return prefix + System.lineSeparator() +
                    cert + System.lineSeparator() +
                    suffix;
        }
        return cert;
    }

    public X509CertificateHolder getCertificate() {
        PEMParser pemParser = new PEMParser(new StringReader(getValidCertString()));
        try {
            return (X509CertificateHolder) pemParser.readObject();
        } catch (IOException e) {
            throw new ApplicationException("Feil i lesing av x509 sertifikat i tillitsliste 602", e);
        }
    }
}

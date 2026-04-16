package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public record ServiceDigitalIdentity(@Valid @NotNull List<String> certs) {

    public String getValidCertString(String cert) {
        String prefix = "-----BEGIN CERTIFICATE-----";
        String suffix = "-----END CERTIFICATE-----";
        if (!cert.startsWith(prefix) || !cert.endsWith(suffix)) {
            return prefix + System.lineSeparator() +
                    cert + System.lineSeparator() +
                    suffix;
        }
        return cert;
    }

    public X509CertificateHolder getCertificate(String cert) {
        String validCert = getValidCertString(cert);
        try (StringReader stringReader = new StringReader(validCert);
             PEMParser pemParser = new PEMParser(stringReader)) {
            return (X509CertificateHolder) pemParser.readObject();
        } catch (IOException e) {
            throw new ApplicationException("Feil i lesing av x509 sertifikat i tillitsliste 602", e);
        }
    }
}

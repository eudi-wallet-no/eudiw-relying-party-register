package no.idporten.eudiw.trustlist.domain;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;

public record TLRpAccessService(TSName name, ZonedDateTime startingTime, String cert) {
    public static final String SERVICE_TYPE_IDENTIFIER_URI_RP_ACCESS = "http://uri.etsi.org/Svc/Svctype/CA/RPaccess";
    public static final String SERVICE_STATUS_URI = "http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/recognisedatnationallevel";

    public TLRpAccessService {
        if (name == null) {
            throw new IllegalArgumentException("RpAccessService: Name must not be null");
        }
        if (startingTime == null || startingTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("RpAccessService: Starting time must not be null and not in the future");
        }
        if (cert == null || cert.isBlank()) {
            throw new IllegalArgumentException("RpAccessService: Certificate must not be null or blank");
        }

    }

    public String getValidCertString() {
        String prefix = "-----BEGIN CERTIFICATE-----";
        String suffix = "-----END CERTIFICATE-----";
        if (!cert.startsWith(prefix) || !cert.endsWith(suffix)) {
            LoggerFactory.getLogger(TLRpAccessService.class).warn("Certificate is not a valid certificate since missing PEM headers/footers, fix it in application.yaml. Trying fix it by adding BEGIN/END-headers like this: \n" +
                    prefix + "\n" +
                    cert + "\n" +
                    suffix);
            return prefix + System.lineSeparator() +
                    cert + System.lineSeparator() +
                    suffix;
        }
        return cert;
    }

    public X509CertificateHolder getCertificate() throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(getValidCertString()));
        return (X509CertificateHolder) pemParser.readObject();
    }
}

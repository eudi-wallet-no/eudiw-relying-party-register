package no.idporten.eudiw.trustlist.domain;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.List;

public record TLService(TSName name, ZonedDateTime startingTime, String serviceTypeIdentifier, String cert) {
    public static final String SERVICE_TYPE_IDENTIFIER_URI_RP_ACCESS = "http://uri.etsi.org/Svc/Svctype/CA/RPaccess";
    public static final String SERVICE_TYPE_IDENTIFIER_URI_EAA = "http://uri.etsi.org/TrstSvc/Svctype/EAA";

    public static final List<String> SUPPORTED_SERVICE_TYPE_IDENTIFIERS = List.of(
            SERVICE_TYPE_IDENTIFIER_URI_RP_ACCESS,
            SERVICE_TYPE_IDENTIFIER_URI_EAA
    );

    public static final String SERVICE_STATUS_URI = "http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/recognisedatnationallevel";

    public TLService {
        if (name == null) {
            throw new IllegalArgumentException("Service: Name must not be null");
        }
        if (startingTime == null || startingTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Service: Starting time must not be null and not in the future '%s'".formatted(startingTime));
        }
        if (cert == null || cert.isBlank()) {
            throw new IllegalArgumentException("Service: Certificate must not be null or blank");
        }
        if (!SUPPORTED_SERVICE_TYPE_IDENTIFIERS.contains(serviceTypeIdentifier)) {
            throw new IllegalArgumentException("Service: Unsupported Service Type Identifier '%s'".formatted(serviceTypeIdentifier));
        }

    }

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

    public X509CertificateHolder getCertificate() throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(getValidCertString()));
        return (X509CertificateHolder) pemParser.readObject();
    }
}

package no.idporten.eudiw.ca.util;

import no.idporten.eudiw.ca.exception.CertificateAuthorityException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateEncodingUtils {

    private CertificateEncodingUtils() {}

    public static String encodeToPem(Object o) {
        try (StringWriter stringWriter = new StringWriter(); PemWriter pemWriter = new PemWriter(stringWriter)) {
            JcaMiscPEMGenerator jcaMiscPEMGenerator = new JcaMiscPEMGenerator(o);
            pemWriter.writeObject(jcaMiscPEMGenerator);
            pemWriter.flush();
            return stringWriter.toString();
        } catch (Exception e) {
            throw new CertificateAuthorityException("server_error", "Failed to encode DER to PEM", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public static <T> T decodeFromPem(String pem, Class<T> clazz) {
        try (PEMParser pemParser = new PEMParser(new StringReader(pem))) {
            return clazz.cast(pemParser.readObject());
        } catch (Exception e) {
            throw new CertificateAuthorityException("invalid_request", "Failed to decode object from PEM", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public static X509Certificate toX509Certificate(X509CertificateHolder certificateHolder) {
        try {
            org.bouncycastle.asn1.x509.Certificate eeX509CertificateStructure = certificateHolder.toASN1Structure();
            CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
            try (InputStream is = new ByteArrayInputStream(eeX509CertificateStructure.getEncoded())) {
                return (X509Certificate) cf.generateCertificate(is);
            }
        } catch (Exception e) {
            throw new CertificateAuthorityException("server_error", "Failed to convert X509 certificate", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

}

package no.eudiw.rp.register.data.accesscertificates;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.eudiw.rp.register.exception.AccessCertificateException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Converter
public class X509CertificateConverter
    implements AttributeConverter<X509Certificate, String> {

    @Override
    public String convertToDatabaseColumn(X509Certificate certificate) {
        return X509CertificateConverter.convert(certificate);
    }

    @Override
    public X509Certificate convertToEntityAttribute(String certificatePemStr) {
        return X509CertificateConverter.convert(certificatePemStr);
    }

    private static final CertificateFactory cf = new CertificateFactory();

    public static String convert(X509Certificate certificate) {
        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            pemWriter.close();
            return writer.toString();
        } catch (IOException e) {
            throw new RegisterServiceException("Failed to encode certificate", e);
        }
    }
    public static X509Certificate convert(String certificatePemStr) {
        try {
            InputStream inStream = new ByteArrayInputStream(certificatePemStr.getBytes());
            Certificate certificate = cf.engineGenerateCertificate(inStream);
            if (!certificate.getType().equals("X.509")) {
                throw new AccessCertificateException("Certificate not X.509");
            }
            return (X509Certificate) certificate;
        } catch (CertificateException e) {
            throw new AccessCertificateException("Invalid certificate", e);
        }
    }
}

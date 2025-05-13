package no.eudiw.rp.register.data.accesscertificates;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.eudiw.rp.register.exception.CertificateConversionException;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.*;
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
            throw new CertificateConversionException("Failed to encode X.509 certificate", e);
        }
    }
    public static X509Certificate convert(String certificatePemStr) {
        try {
            InputStream inStream = new ByteArrayInputStream(certificatePemStr.getBytes());
            Certificate certificate = cf.engineGenerateCertificate(inStream);
            if (!certificate.getType().equals("X.509")) {
                throw new CertificateConversionException("Certificate valid but is not X.509");
            }
            return (X509Certificate) certificate;
        } catch (CertificateException e) {
            throw new CertificateConversionException("Invalid X.509 certificate", e);
        }
    }
}

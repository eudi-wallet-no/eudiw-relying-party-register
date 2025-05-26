package no.idporten.eudiw.rp.admin.service.accesscertificates;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class X509CertificateConverter {

    private static final CertificateFactory cf = new CertificateFactory();

    public static String convert(X509Certificate certificate) {
        try (StringWriter writer = new StringWriter();
             JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        ) {
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            pemWriter.close();
            return writer.toString();
        } catch (IOException e) {
            throw new CertificateConversionException("Failed to encode X.509 certificate", e);
        }
    }
    public static X509Certificate convert(String certificatePemStr) {
        try (InputStream inStream = new ByteArrayInputStream(certificatePemStr.getBytes())) {
            Certificate certificate = cf.engineGenerateCertificate(inStream);
            if (!certificate.getType().equals("X.509")) {
                throw new CertificateConversionException("Certificate valid but is not X.509");
            }
            return (X509Certificate) certificate;
        } catch (CertificateException e) {
            throw new CertificateConversionException("Invalid X.509 certificate", e);
        }
        catch (IOException e) {
            throw new CertificateConversionException("Certificate conversion failed", e);
        }
    }
}

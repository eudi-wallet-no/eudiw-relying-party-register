package no.eudiw.rp.register.data.accesscertificates;

import no.eudiw.rp.register.exception.CertificateConversionException;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class PKCS10CertificationRequestConverter {

    public static String convert(PKCS10CertificationRequest csr) {
        try (
            StringWriter stringWriter = new StringWriter();
            PemWriter pemWriter = new PemWriter(stringWriter)
        ) {
            pemWriter.writeObject(new JcaMiscPEMGenerator(csr));
            pemWriter.flush();
            return stringWriter.toString();
        }
        catch (IOException e) {
            throw new CertificateConversionException("Failed to encode PKCS10 CSR", e);
        }
    }

    public static PKCS10CertificationRequest convert(String csrPemStr) {
        try {
            PKCS10CertificationRequest csr =
                (PKCS10CertificationRequest)
                    new PEMParser(new StringReader(csrPemStr)).readObject();
            if (csr == null) {
                throw new CertificateConversionException("Invalid PKCS10 CSR");
            }
            return csr;
        } catch (IOException e) {
            throw new CertificateConversionException("Failed to convert PKCS10 from PEM", e);
        }
    }
}

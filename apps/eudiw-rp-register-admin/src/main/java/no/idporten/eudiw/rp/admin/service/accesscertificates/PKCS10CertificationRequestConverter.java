package no.idporten.eudiw.rp.admin.service.accesscertificates;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

@Component
public class PKCS10CertificationRequestConverter
    implements Converter<String, PKCS10CertificationRequest> {

    public static String toString(PKCS10CertificationRequest csr) {
        try (
            StringWriter stringWriter = new StringWriter();
            PemWriter pemWriter = new PemWriter(stringWriter);
        ) {
            pemWriter.writeObject(new JcaMiscPEMGenerator(csr));
            pemWriter.flush();
            return stringWriter.toString();
        }
        catch (IOException e) {
            throw new CertificateConversionException("Failed to encode PKCS10 CSR", e);
        }
    }

    public static PKCS10CertificationRequest fromString(String csrPemStr) {
        try (
            StringReader strReader = new StringReader(csrPemStr);
            PEMParser pemParser = new PEMParser(strReader);
        ) {
            PKCS10CertificationRequest csr =
                (PKCS10CertificationRequest) pemParser.readObject();
            if (csr == null) {
                throw new CertificateConversionException("Invalid PKCS10 CSR");
            }
            return csr;
        } catch (IOException e) {
            throw new CertificateConversionException("Failed to convert PKCS10 from PEM", e);
        }
    }

    @Override
    public PKCS10CertificationRequest convert(@NonNull String csrPemStr) {
        return PKCS10CertificationRequestConverter.fromString(csrPemStr);
    }
}


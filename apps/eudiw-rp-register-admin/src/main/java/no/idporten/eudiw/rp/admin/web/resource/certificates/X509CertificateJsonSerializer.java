package no.idporten.eudiw.rp.admin.web.resource.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;

public class X509CertificateJsonSerializer
    extends JsonSerializer<X509Certificate> {

    @Override
    public void serialize(X509Certificate certificate,
                          JsonGenerator jsonGen,
                          SerializerProvider _unused) throws IOException {
        jsonGen.writeString(convert(certificate));
    }

    public static String convert(X509Certificate certificate) {
        try (
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        ) {
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            pemWriter.close();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode X.509 certificate", e);
        }
    }
}

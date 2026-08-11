package no.idporten.eudiw.rp.admin.web.resource.certificates;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;

public class X509CertificateJsonSerializer
    extends ValueSerializer<X509Certificate> {

    @Override
    public void serialize(X509Certificate certificate,
                          JsonGenerator jsonGen,
                          SerializationContext _unused){
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

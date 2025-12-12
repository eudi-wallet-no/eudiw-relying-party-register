package no.eudiw.rp.register.api.resource.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import no.eudiw.rp.register.data.entity.certificates.X509CertificateConverter;

import java.io.IOException;
import java.security.cert.X509Certificate;

public class X509CertificateJsonSerializer
    extends JsonSerializer<X509Certificate> {

    @Override
    public void serialize(X509Certificate certificate,
                          JsonGenerator jsonGen,
                          SerializerProvider _unused) throws IOException {
        jsonGen.writeString(X509CertificateConverter.convert(certificate));
    }
}

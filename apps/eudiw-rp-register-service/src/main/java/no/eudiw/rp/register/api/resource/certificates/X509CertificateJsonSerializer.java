package no.eudiw.rp.register.api.resource.certificates;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import no.eudiw.rp.register.data.entity.certificates.X509CertificateConverter;

import java.security.cert.X509Certificate;

public class X509CertificateJsonSerializer
    extends ValueSerializer<X509Certificate> {

    @Override
    public void serialize(X509Certificate certificate,
                          JsonGenerator jsonGen,
                          SerializationContext _unused)  {
        jsonGen.writeString(X509CertificateConverter.convert(certificate));
    }
}

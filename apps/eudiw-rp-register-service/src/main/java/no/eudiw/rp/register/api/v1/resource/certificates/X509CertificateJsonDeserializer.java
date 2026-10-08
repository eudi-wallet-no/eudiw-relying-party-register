package no.eudiw.rp.register.api.v1.resource.certificates;


import no.eudiw.rp.register.domain.certificates.X509CertificateConverter;
import no.eudiw.rp.register.exception.CertificateConversionException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.security.cert.X509Certificate;

public class X509CertificateJsonDeserializer
    extends ValueDeserializer<X509Certificate> {

    @Override
    public X509Certificate deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext) {
        try {
            String csrPemStr = jsonParser.getValueAsString();
            return X509CertificateConverter.convert(csrPemStr);
        } catch (Exception e) {
            throw new CertificateConversionException(
                "Failed to parse X.509 PEM from json", e);
        }
    }
}

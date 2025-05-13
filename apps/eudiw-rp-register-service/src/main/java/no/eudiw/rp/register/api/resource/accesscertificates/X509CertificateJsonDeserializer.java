package no.eudiw.rp.register.api.resource.accesscertificates;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import no.eudiw.rp.register.data.accesscertificates.X509CertificateConverter;
import no.eudiw.rp.register.exception.CertificateConversionException;

import java.io.IOException;
import java.security.cert.X509Certificate;

public class X509CertificateJsonDeserializer
    extends JsonDeserializer<X509Certificate> {

    @Override
    public X509Certificate deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext) {
        try {
            String csrPemStr = jsonParser.getValueAsString();
            return X509CertificateConverter.convert(csrPemStr);
        } catch (IOException e) {
            throw new CertificateConversionException(
                "Failed to parse X.509 PEM from json", e);
        }
    }
}

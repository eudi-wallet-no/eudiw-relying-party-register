package no.idporten.eudiw.rp.admin.web.resource.certificates;

import no.idporten.eudiw.rp.admin.service.accesscertificates.CertificateConversionException;
import no.idporten.eudiw.rp.admin.service.accesscertificates.X509CertificateConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ValueDeserializer;

import java.security.cert.X509Certificate;

public class X509CertificateJsonDeserializer
    extends ValueDeserializer<X509Certificate> {

    @Override
    public X509Certificate deserialize(tools.jackson.core.JsonParser jsonParser, tools.jackson.databind.DeserializationContext context) throws JacksonException {
        try {
            String csrPemStr = jsonParser.getValueAsString();
            return X509CertificateConverter.fromPem(csrPemStr);
        } catch (JacksonException e) {
            throw new CertificateConversionException(
                    "Failed to parse X.509 PEM from json", e);
        }
    }
}

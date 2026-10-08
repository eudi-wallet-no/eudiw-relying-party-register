package no.eudiw.rp.register.api.v1.resource.certificates;


import no.eudiw.rp.register.domain.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.exception.CertificateConversionException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;


public class PKCS10CertificationRequestJsonDeserializer
    extends ValueDeserializer<PKCS10CertificationRequest> {

    @Override
    public PKCS10CertificationRequest deserialize(
        JsonParser jsonParser,
        DeserializationContext _unused) {
        try {
            String csrPemStr = jsonParser.getValueAsString();
            return PKCS10CertificationRequestConverter.convert(csrPemStr);
        } catch (Exception e) {
            throw new CertificateConversionException(
                "Failed to parse PKCS10 CSR PEM from json", e);
        }
    }
}

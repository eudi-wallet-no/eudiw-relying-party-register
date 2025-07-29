package no.eudiw.rp.register.api.resource.certificates;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import no.eudiw.rp.register.data.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.exception.CertificateConversionException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;

public class PKCS10CertificationRequestJsonDeserializer
    extends JsonDeserializer<PKCS10CertificationRequest> {

    @Override
    public PKCS10CertificationRequest deserialize(
        JsonParser jsonParser,
        DeserializationContext _unused) {
        try {
            String csrPemStr = jsonParser.getValueAsString();
            return PKCS10CertificationRequestConverter.convert(csrPemStr);
        } catch (IOException e) {
            throw new CertificateConversionException(
                "Failed to parse PKCS10 CSR PEM from json", e);
        }
    }
}

package no.eudiw.rp.register.api.resource.certificates;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import no.eudiw.rp.register.data.entity.certificates.PKCS10CertificationRequestConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;

public class PKCS10CertificationRequestJsonSerializer
    extends JsonSerializer<PKCS10CertificationRequest> {

    @Override
    public void serialize(
        PKCS10CertificationRequest csr,
        JsonGenerator jsonGen,
        SerializerProvider serializerProvider) throws IOException {
        jsonGen.writeString(PKCS10CertificationRequestConverter.convert(csr));
    }
}

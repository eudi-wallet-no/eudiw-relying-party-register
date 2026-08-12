package no.eudiw.rp.register.api.resource.certificates;


import no.eudiw.rp.register.data.entity.certificates.PKCS10CertificationRequestConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;


public class PKCS10CertificationRequestJsonSerializer
    extends ValueSerializer<PKCS10CertificationRequest> {

    @Override
    public void serialize(
        PKCS10CertificationRequest csr,
        JsonGenerator jsonGen,
        SerializationContext serializerProvider){
        jsonGen.writeString(PKCS10CertificationRequestConverter.convert(csr));
    }
}

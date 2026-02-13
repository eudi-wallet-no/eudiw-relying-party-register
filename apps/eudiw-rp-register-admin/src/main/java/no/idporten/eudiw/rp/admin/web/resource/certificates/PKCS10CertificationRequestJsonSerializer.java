package no.idporten.eudiw.rp.admin.web.resource.certificates;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;

public class PKCS10CertificationRequestJsonSerializer
    extends ValueSerializer<PKCS10CertificationRequest> {

    @Override
    public void serialize(
        PKCS10CertificationRequest csr,
        JsonGenerator jsonGen,
        SerializationContext _unused){
        jsonGen.writeString(PKCS10CertificationRequestConverter.toString(csr));
    }
}

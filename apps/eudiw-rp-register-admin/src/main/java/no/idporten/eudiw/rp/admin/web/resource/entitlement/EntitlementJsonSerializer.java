package no.idporten.eudiw.rp.admin.web.resource.entitlement;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;

import java.io.IOException;

public class EntitlementJsonSerializer
    extends JsonSerializer<RelyingPartyEntitlement> {
    @Override
    public void serialize(
        RelyingPartyEntitlement entitlement,
        JsonGenerator jsonGen,
        SerializerProvider _unused) throws IOException {
        jsonGen.writeString(entitlement.getUri());
    }
}

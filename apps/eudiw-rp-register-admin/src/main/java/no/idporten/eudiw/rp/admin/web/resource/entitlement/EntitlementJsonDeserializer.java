package no.idporten.eudiw.rp.admin.web.resource.entitlement;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;

import java.io.IOException;

public class EntitlementJsonDeserializer
    extends JsonDeserializer<RelyingPartyEntitlement> {
    @Override
    public RelyingPartyEntitlement deserialize(
        JsonParser jsonParser,
        DeserializationContext _unused) throws IOException {
        return RelyingPartyEntitlement.fromString(jsonParser.getValueAsString());
    }
}

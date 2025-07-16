package no.eudiw.rp.register.api.resource.entitlement;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import no.eudiw.rp.register.data.entitlement.Entitlement;

import java.io.IOException;

public class EntitlementJsonDeserializer
    extends JsonDeserializer<Entitlement> {
    @Override
    public Entitlement deserialize(
        JsonParser jsonParser,
        DeserializationContext _unused) throws IOException {
        return Entitlement.fromString(jsonParser.getValueAsString());
    }
}

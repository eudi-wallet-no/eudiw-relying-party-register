package no.eudiw.rp.register.api.resource.entitlement;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import no.eudiw.rp.register.data.entitlement.Entitlement;

import java.io.IOException;

public class EntitlementJsonSerializer
    extends JsonSerializer<Entitlement> {
    @Override
    public void serialize(
        Entitlement entitlement,
        JsonGenerator jsonGen,
        SerializerProvider _unused) throws IOException {
        jsonGen.writeString(entitlement.getValue());
    }
}

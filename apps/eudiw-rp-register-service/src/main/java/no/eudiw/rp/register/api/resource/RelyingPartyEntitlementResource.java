package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.eudiw.rp.register.api.resource.entitlement.*;
import no.eudiw.rp.register.data.entitlement.Entitlement;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    @JsonSerialize(using = EntitlementJsonSerializer.class)
    @JsonDeserialize(using = EntitlementJsonDeserializer.class)
    Entitlement entitlement
) { }

package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;
import no.idporten.eudiw.rp.admin.web.resource.entitlement.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    @JsonSerialize(using = EntitlementJsonSerializer.class)
    @JsonDeserialize(using = EntitlementJsonDeserializer.class)
    RelyingPartyEntitlement entitlement
) { }

package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    String entitlement,

    @JsonProperty("display_name")
    String displayName
) {
    public RelyingPartyEntitlementResource(String entitlement) {
        this(entitlement, entitlement);
    }
}

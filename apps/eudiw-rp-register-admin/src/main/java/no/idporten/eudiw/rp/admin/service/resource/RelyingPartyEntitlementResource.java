package no.idporten.eudiw.rp.admin.service.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    String entitlement
) { }

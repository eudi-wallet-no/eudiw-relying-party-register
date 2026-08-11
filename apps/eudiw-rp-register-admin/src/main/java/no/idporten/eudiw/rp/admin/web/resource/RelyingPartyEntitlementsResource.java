package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementsResource(
    @JsonProperty("entitlements")
    List<RelyingPartyEntitlementResource> entitlements
) { }

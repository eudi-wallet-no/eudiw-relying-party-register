package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchRelyingPartyResource(
    @JsonProperty("search_term")
    String searchTerm,
    @JsonProperty("include_inactive")
    boolean includeInactive,
    @JsonProperty("required_entitlements")
    List<RelyingPartyEntitlementResource> requiredEntitlements
) { }

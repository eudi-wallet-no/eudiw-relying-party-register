package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchRelyingPartyResource(
    @JsonProperty("orgno")
    String orgno,
    @JsonProperty("name")
    String name,
    @JsonProperty("public_sector")
    Boolean publicSector,
    @JsonProperty("include_inactive")
    boolean includeInactive
) { }

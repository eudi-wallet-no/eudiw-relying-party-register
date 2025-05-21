package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchRelyingPartyResource(
    @JsonProperty("search_term")
    String searchTerm,
    @JsonProperty("include_inactive")
    boolean includeInactive
) { }

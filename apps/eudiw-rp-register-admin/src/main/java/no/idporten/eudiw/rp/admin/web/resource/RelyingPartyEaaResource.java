package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEaaResource(
    @JsonProperty("namespace")
    String namespace,
    @JsonProperty("intent")
    String intent
) { }

package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartiesResource(
    @JsonProperty(value = "relying_parties", required = true)
    List<RelyingPartyResource> relyingParties
) { }

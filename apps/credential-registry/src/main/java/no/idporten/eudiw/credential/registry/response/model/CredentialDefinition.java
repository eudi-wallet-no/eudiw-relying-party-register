package no.idporten.eudiw.credential.registry.response.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

public record CredentialDefinition(
    @JsonProperty("type")
    List<String> type,
    @JsonProperty("@context")
    List<URI> context
) {


}

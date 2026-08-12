package no.idporten.eudiw.credential.registry.response.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CredentialDefinition(
    @JsonProperty("type")
    List<String> type,
    @JsonProperty("@context")
    List<URI> context
) {


}

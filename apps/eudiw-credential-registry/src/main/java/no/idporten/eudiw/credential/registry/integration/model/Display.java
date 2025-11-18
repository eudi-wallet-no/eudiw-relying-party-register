package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Display(
        @NotBlank
        @JsonProperty("name")
        String name,
        @NotBlank
        @JsonProperty("locale")
        String locale,
        @JsonProperty("description")
        String description
) {
}

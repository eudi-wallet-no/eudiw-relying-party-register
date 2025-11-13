package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown=true)
public record ProofTypesSupported(
        @NotNull
        @JsonProperty("jwt")
        ProofSigningAlgValuesSupported jwt
) {
}

package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProofSigningAlgValuesSupported(
        @NotNull
        @JsonProperty("proof_signing_alg_values_supported")
        List<@NotBlank String> proofSigningAlgValuesSupported
) {
}

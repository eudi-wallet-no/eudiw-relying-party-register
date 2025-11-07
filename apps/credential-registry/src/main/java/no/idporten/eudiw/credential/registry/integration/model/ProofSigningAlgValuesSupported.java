package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProofSigningAlgValuesSupported(
        @JsonProperty("proof_signing_alg_values_supported")
        List<String> proofSigningAlgValuesSupported
) {
}

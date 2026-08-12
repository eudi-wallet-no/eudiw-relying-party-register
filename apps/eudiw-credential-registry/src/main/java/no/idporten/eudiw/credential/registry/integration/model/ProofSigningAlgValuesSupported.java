package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProofSigningAlgValuesSupported {
    @JsonProperty("proof_signing_alg_values_supported")
    List<String> proofSigningAlgValuesSupported;

    public ProofSigningAlgValuesSupported() {

    }

    public ProofSigningAlgValuesSupported(List<String> proofSigningAlgValuesSupported) {
        this.proofSigningAlgValuesSupported = proofSigningAlgValuesSupported;
    }

    public void setProofSigningAlgValuesSupported(List<String> proofSigningAlgValuesSupported) {
        this.proofSigningAlgValuesSupported = proofSigningAlgValuesSupported;
    }

    public List<String> getProofSigningAlgValuesSupported() {
        return proofSigningAlgValuesSupported;
    }
}

package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialConfiguration(
        @JsonProperty("doctype")
        String doctype,
        @JsonProperty("vct")
        String vct,
        @JsonProperty("scope")
        String scope,
        @JsonProperty("format")
        @NotBlank
        String format,
        @NotNull
        @JsonProperty("credential_metadata")
        CredentialMetadata credentialMetadata,
        @NotNull
        @JsonProperty("proof_types_supported")
        ProofTypesSupported proofTypesSupported
) {

    public String doctype() {
        return this.doctype != null ? this.doctype : this.vct;
    }

}

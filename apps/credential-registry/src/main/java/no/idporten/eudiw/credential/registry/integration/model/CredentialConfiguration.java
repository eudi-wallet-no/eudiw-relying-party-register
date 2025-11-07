package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialConfiguration(
        @JsonProperty("doctype")
        String doctype,
        @JsonProperty("vct")
        String vct,
        @JsonProperty("scope")
        String scope,
        @JsonProperty("format")
        String format,
        @JsonProperty("credential_metadata")
        CredentialMetadata credentialMetadata,
        @JsonProperty("proof_types_supported")
        ProofTypesSupported proofTypesSupported
) {

}

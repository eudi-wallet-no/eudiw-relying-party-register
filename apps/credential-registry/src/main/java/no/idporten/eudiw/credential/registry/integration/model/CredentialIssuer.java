package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialIssuer(
        @JsonProperty("credential_issuer")
        String credentialIssuer,
        @JsonProperty("credential_configurations_supported")
        Map<String, CredentialConfiguration> credentialConfiguration
) {
}

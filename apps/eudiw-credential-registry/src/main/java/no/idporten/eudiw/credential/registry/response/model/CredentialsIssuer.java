package no.idporten.eudiw.credential.registry.response.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CredentialsIssuer (
        @JsonProperty("credential_issuer")
        String credentialIssuer,
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,
        @JsonProperty("credential_type")
        String credentialType,
        @JsonProperty("format")
        String format,
        @JsonProperty("credential_metadata")
        CredentialMetadata credentialMetadata
){
}

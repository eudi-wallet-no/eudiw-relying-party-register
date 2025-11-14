package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialIssuer(
        @JsonProperty("credential_issuer")
        @NotBlank
        String credentialIssuer,
        @NotNull
        @JsonProperty("credential_configurations_supported")
        Map<String, @Valid CredentialConfiguration> credentialConfiguration,
        @NotEmpty
        @JsonProperty("display")
        List<@Valid Display> display
) {
}






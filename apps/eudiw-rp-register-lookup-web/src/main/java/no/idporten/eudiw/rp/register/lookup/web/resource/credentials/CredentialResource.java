package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialResource(
    @NotNull
    @Pattern(regexp = "mso_mdoc|dc\\+sd-jwt", message = "unrecognized_format")
    @JsonProperty(value = "format", required = true)
    String format,

    @NotBlank(message = "invalid_issuer")
    @JsonProperty(value = "credential_issuer", required = true)
    String issuer,

    @NotNull
    @JsonProperty(value = "display", required = true)
    List<Display> issuerDisplays,

    @NotBlank(message = "invalid_configuration_id")
    @JsonProperty(value = "credential_configuration_id", required = true)
    String configurationId,

    @NotBlank(message = "invalid_credential_type")
    @JsonProperty(value = "credential_type", required = true)
    String credentialType,

    @NotNull(message = "invalid_metadata")
    @JsonProperty(value = "credential_metadata", required = true)
    CredentialMetadata metadata
) {
    public String getIssuerDisplay(String locale) {
        return Display.getDisplayForLocale(locale, this.issuerDisplays);
    }
}

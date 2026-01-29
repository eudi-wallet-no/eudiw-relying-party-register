package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.With;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@With
public record CredentialResource(
    @NotNull
    @Pattern(regexp = "mso_mdoc|dc\\+sd-jwt|jwt_vc_json", message = "unrecognized_format")
    @JsonProperty(value = "format", required = true)
    String format,

    @NotBlank(message = "invalid_issuer")
    @JsonProperty(value = "credential_issuer", required = true)
    String issuer,

    @Size(min = 1, message = "empty_display") // like @NotEmpty but allows null
    @JsonProperty("display")
    List<@NotNull @Valid Display> issuerDisplays,

    @NotBlank(message = "invalid_configuration_id")
    @JsonProperty(value = "credential_configuration_id", required = true)
    String configurationId,

    @Pattern(regexp = "\\s*\\S+\\s*", message = "nonnull_and_blank_credential_type") // like @NotBlank, but allow null
    @JsonProperty("credential_type")
    String credentialType,

    @NotNull(message = "null_metadata")
    @Valid
    @JsonProperty(value = "credential_metadata", required = true)
    CredentialMetadata metadata
) {
    public String getIssuerDisplayName(String locale) {
        return this.issuerDisplays != null
                   ? Display.getDisplayNameForLocale(locale, this.issuerDisplays)
                   : this.issuer;
    }

    public String getCredentialTypeDisplayName(String locale) {
        return this.metadata().credentialTypeDisplays() != null
                   ? Display.getDisplayNameForLocale(locale, this.metadata().credentialTypeDisplays())
                   : this.credentialType;
    }
    public String getCredentialTypeDescription(String locale) {
        return this.metadata.credentialTypeDisplays() != null
                   ? Display.getDescriptionForLocale(locale, this.metadata().credentialTypeDisplays())
                   : null;
    }

    @JsonIgnore
    @AssertTrue(message = "credential_type_missing")
    @SuppressWarnings("unused") // used by jakarta
    public boolean isVcJsonOrNonNullCredentialType() {
        return this.format.equals("jwt_vc_json") || this.credentialType != null;
    }
}

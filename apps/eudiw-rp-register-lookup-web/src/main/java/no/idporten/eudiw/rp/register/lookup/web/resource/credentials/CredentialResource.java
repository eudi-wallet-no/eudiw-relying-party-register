package no.idporten.eudiw.rp.register.lookup.web.resource.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@With
public class CredentialResource {
    @NotNull
    @Pattern(regexp = "mso_mdoc|dc\\+sd-jwt|jwt_vc_json", message = "unrecognized_format")
    @JsonProperty(value = "format", required = true)
    private String format;

    @NotBlank(message = "invalid_issuer")
    @JsonProperty(value = "credential_issuer", required = true)
    private String issuer;

    @JsonProperty("display")
    private List<@NotNull @Valid Display> issuerDisplays = new ArrayList<>();

    @NotBlank(message = "invalid_configuration_id")
    @JsonProperty(value = "credential_configuration_id", required = true)
    private String configurationId;

    @NotBlank(message = "credential_type_missing")
    @JsonProperty(value = "credential_type", required = true)
    private String credentialType;

    @Valid
    @JsonProperty("credential_metadata")
    private CredentialMetadata metadata = new CredentialMetadata();

    public String getIssuerDisplayName(String locale) {
        return Display.getDisplayNameForLocale(locale, this.issuerDisplays, this.issuer);
    }
    public String getCredentialTypeDisplayName(String locale) {
        if (this.metadata == null) {
            return this.credentialType;
        }
        return Display.getDisplayNameForLocale(locale, this.getMetadata().getCredentialTypeDisplays(), this.credentialType);
    }
    public String getCredentialTypeDescription(String locale) {
        if (this.metadata == null) {
            return null;
        }
        return Display.getDescriptionForLocale(locale, this.getMetadata().getCredentialTypeDisplays());
    }
}

package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialConfiguration{
    @Pattern(regexp = "mso_mdoc|dc\\+sd-jwt|jwt_vc_json", message = "unrecognized_format")
    @JsonProperty("format")
    @NotBlank
    private String format;
    @JsonProperty("scope")
    private String scope = "";
    @JsonProperty("credential_signing_alg_values_supported")
    private List<String> credentialSigningAlgValuesSupported = null;
    @JsonProperty("cryptographic_binding_methods_supported")
    private List<String> cryptographicBindingMethodsSupported = null;
    @Valid
    @JsonProperty("credential_definition")
    private CredentialDefinition credentialDefinition = null;
    @JsonProperty("proof_types_supported")
    private ProofTypesSupported proofTypesSupported= new ProofTypesSupported(new ProofSigningAlgValuesSupported(new ArrayList<>()));
    @JsonProperty("credential_metadata")
    @Valid
    private CredentialMetadata credentialMetadata = null;
    @JsonProperty("doctype")
    private String doctype;
    @JsonProperty("vct")
    private String vct;

    public CredentialConfiguration() {}

    public CredentialConfiguration(String format) {
        this.format = format;
    }

    public String findTypeByFormat() {
        return switch (this.format) {
            case "mso_mdoc" -> this.doctype;
            case "dc+sd-jwt" -> this.vct;
            case "jwt_vc_json" -> this.credentialDefinition.getType().get(1);
            default -> null;
        };
    }

    public void setScope(String scope){
        this.scope = scope;
    }

    public String getScope() {
        return this.scope;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return this.format;
    }

    public void setCredentialSigningAlgValuesSupported(List<String> credentialSigningAlgValuesSupported) {
        this.credentialSigningAlgValuesSupported = credentialSigningAlgValuesSupported;
    }

    public List<String> getCredentialSigningAlgValuesSupported() {
        return this.credentialSigningAlgValuesSupported;
    }

    public void setCryptographicBindingMethodsSupported(List<String> cryptographicBindingMethodsSupported) {
        this.cryptographicBindingMethodsSupported = cryptographicBindingMethodsSupported;
    }

    public List<String> getCryptographicBindingMethodsSupported() {
        return this.cryptographicBindingMethodsSupported;
    }

    public void setProofTypesSupported(ProofTypesSupported proofTypesSupported) {
        this.proofTypesSupported = proofTypesSupported;
    }

    public ProofTypesSupported getProofTypesSupported() {
        return this.proofTypesSupported;
    }

    public void setCredentialMetadata(CredentialMetadata credentialMetadata) {
        this.credentialMetadata = credentialMetadata;
    }

    public CredentialMetadata getCredentialMetadata() {
        return this.credentialMetadata;
    }

    public void setCredentialDefinition(CredentialDefinition credentialDefinition) {
            this.credentialDefinition = credentialDefinition;
    }

    public CredentialDefinition getCredentialDefinition() {
        return this.credentialDefinition;
    }
}

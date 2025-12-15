package no.idporten.eudiw.credential.registry.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialConfiguration{
    @JsonProperty("format")
    @NotBlank
    private String format;
    @JsonProperty("scope")
    private String scope = "";
    @JsonProperty("credential_signing_alg_values_supported")
    private List<String> credentialSigningAlgValuesSupported = new ArrayList();
    @JsonProperty("cryptographic_binding_methods_supported")
    private List<String> cryptographicBindingMethodsSupported = new ArrayList();
    @JsonProperty("proof_types_supported")
    private ProofTypesSupported proofTypesSupported= new ProofTypesSupported(new ProofSigningAlgValuesSupported(new ArrayList<>()));
    @JsonProperty("credential_metadata")
    private CredentialMetadata credentialMetadata = new CredentialMetadata();
    @JsonProperty("doctype") //hva skal man gjøre med dette
    private String doctype;
    @JsonProperty("vct") //hva skal man gjøre med dette
    private String vct;

    public CredentialConfiguration() {}

    public CredentialConfiguration(String format) {
        this.format = format;
    }

    public String doctype() {
        return this.doctype != null ? this.doctype : this.vct;
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

}

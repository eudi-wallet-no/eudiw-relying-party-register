package no.eudiw.rp.register.api.v1.resource.relyingparty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelyingPartyEntitlementResource {
    @JsonProperty(value = "entitlement", required = true)
    private String entitlement;

    @JsonProperty("display_name")
    private String displayName;

    private String credentialIssuerUrl;
    private boolean credentialIssuerUrlPresent;

    @JsonCreator
    public RelyingPartyEntitlementResource() { }

    public RelyingPartyEntitlementResource(String entitlement, String displayName, String credentialIssuerUrl) {
        this.entitlement = entitlement;
        this.displayName = displayName;
        setCredentialIssuerUrl(credentialIssuerUrl);
    }

    public String entitlement() {
        return entitlement;
    }

    public String displayName() {
        return displayName;
    }

    @JsonProperty("credential_issuer_url")
    public String credentialIssuerUrl() {
        return credentialIssuerUrl;
    }

    @JsonProperty("credential_issuer_url")
    public void setCredentialIssuerUrl(String credentialIssuerUrl) {
        this.credentialIssuerUrl = credentialIssuerUrl;
        this.credentialIssuerUrlPresent = true;
    }

    public boolean credentialIssuerUrlPresent() {
        return credentialIssuerUrlPresent;
    }
}

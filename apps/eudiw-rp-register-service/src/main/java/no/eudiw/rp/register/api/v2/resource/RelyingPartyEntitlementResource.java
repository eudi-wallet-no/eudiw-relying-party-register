package no.eudiw.rp.register.api.v2.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;

import java.net.URI;

public class RelyingPartyEntitlementResource {
    @JsonProperty
    @NotBlank
    private String entitlement;

    @Schema(description = "The service keeps this credential issuer URL for the role.")
    private String credentialIssuerUrl;
    private boolean credentialIssuerUrlPresent;

    @JsonCreator
    public RelyingPartyEntitlementResource() { }

    public RelyingPartyEntitlementResource(String entitlement, String credentialIssuerUrl) {
        this.entitlement = entitlement;
        setCredentialIssuerUrl(credentialIssuerUrl);
    }

    public String entitlement() {
        return entitlement;
    }

    @JsonProperty("credentialIssuerUrl")
    public String credentialIssuerUrl() {
        return credentialIssuerUrl;
    }

    @JsonProperty("credentialIssuerUrl")
    public void setCredentialIssuerUrl(String credentialIssuerUrl) {
        this.credentialIssuerUrl = credentialIssuerUrl;
        this.credentialIssuerUrlPresent = true;
    }

    public boolean credentialIssuerUrlPresent() {
        return credentialIssuerUrlPresent;
    }

    @AssertTrue(message = "invalid_credential_issuer_url")
    public boolean hasValidCredentialIssuerUrl() {
        if (credentialIssuerUrl == null) {
            return true;
        }
        try {
            URI issuerUri = URI.create(credentialIssuerUrl);
            return "https".equals(issuerUri.getScheme()) && V2ResourceValidation.hasText(issuerUri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public RelyingPartyEntitlement toDomain() {
        return new RelyingPartyEntitlement(entitlement);
    }
}

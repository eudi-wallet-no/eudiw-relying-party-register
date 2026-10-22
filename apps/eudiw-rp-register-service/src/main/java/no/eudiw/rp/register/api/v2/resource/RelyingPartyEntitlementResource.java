package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record RelyingPartyEntitlementResource(
    @NotBlank String entitlement,
    String credentialIssuerUrl
) {
    @AssertTrue(message = "invalid_credential_issuer_url")
    public boolean hasValidCredentialIssuerUrl() {
        return credentialIssuerUrl == null || V2ResourceValidation.isAbsoluteUri(credentialIssuerUrl);
    }
}

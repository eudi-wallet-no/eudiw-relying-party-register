package no.eudiw.rp.register.api.v2.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;

import java.net.URI;

public record RelyingPartyEntitlementResource(
    @NotBlank String entitlement,
    @Schema(description = "Optional credential issuer URL. When supplied, must use https and have a host.")
    String credentialIssuerUrl
) {
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
        return new RelyingPartyEntitlement(entitlement, credentialIssuerUrl);
    }
}

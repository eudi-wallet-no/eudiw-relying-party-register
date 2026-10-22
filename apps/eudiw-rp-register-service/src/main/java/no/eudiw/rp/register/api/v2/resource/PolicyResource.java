package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record PolicyResource(
    @NotBlank String type,
    @NotBlank String policyURI
) {
    @AssertTrue(message = "invalid_policy_uri")
    public boolean hasValidPolicyUri() {
        return V2ResourceValidation.isAbsoluteUri(policyURI);
    }
}

package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.Policy;

public record PolicyResource(
    @NotBlank String type,
    @NotBlank String policyURI
) {
    @AssertTrue(message = "invalid_policy_uri")
    public boolean hasValidPolicyUri() {
        return V2ResourceValidation.isAbsoluteUri(policyURI);
    }

    public Policy toDomain() {
        return new Policy(type, policyURI);
    }
}

package no.eudiw.rp.register.api.v2.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record IntermediaryReferenceResource(
    @NotBlank String serviceIdentifier,
    @NotBlank String serviceTradeName,
    @JsonProperty("registryURI") @NotBlank String registryUri
) {
    @AssertTrue(message = "invalid_intermediary_registry_uri")
    public boolean hasValidRegistryUri() {
        return V2ResourceValidation.isAbsoluteUri(registryUri);
    }
}

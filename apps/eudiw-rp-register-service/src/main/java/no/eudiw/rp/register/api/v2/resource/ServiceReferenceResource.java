package no.eudiw.rp.register.api.v2.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.domain.ServiceReference;

import java.util.List;

public record ServiceReferenceResource(
    @JsonProperty(required = true) @NotEmpty List<@NotNull @Valid IdentifierResource> identifier,
    @JsonProperty(required = true) @NotBlank String serviceIdentifier,
    @JsonProperty(value = "registryURI", required = true) @NotBlank @Schema(format = "uri") String registryUri
) {
    @AssertTrue(message = "invalid_service_reference_registry_uri")
    public boolean hasValidRegistryUri() {
        return V2ResourceValidation.isAbsoluteUri(registryUri);
    }

    public ServiceReference toDomain() {
        return new ServiceReference(
            identifier.stream().map(IdentifierResource::toDomain).toList(), serviceIdentifier, registryUri);
    }

    public static ServiceReferenceResource from(ServiceReference reference) {
        return new ServiceReferenceResource(
            reference.identifier().stream()
                .map(value -> new IdentifierResource(value.type(), value.identifier())).toList(),
            reference.serviceIdentifier(),
            reference.registryURI());
    }
}

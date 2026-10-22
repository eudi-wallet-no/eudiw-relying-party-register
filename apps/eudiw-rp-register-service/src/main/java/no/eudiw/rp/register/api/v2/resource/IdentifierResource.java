package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.Identifier;

public record IdentifierResource(
    @NotBlank String type,
    @NotBlank String identifier
) {
    public Identifier toDomain() {
        return new Identifier(type, identifier);
    }
}

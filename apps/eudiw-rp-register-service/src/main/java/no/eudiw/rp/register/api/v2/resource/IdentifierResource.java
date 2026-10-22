package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;

public record IdentifierResource(
    @NotBlank String type,
    @NotBlank String identifier
) { }

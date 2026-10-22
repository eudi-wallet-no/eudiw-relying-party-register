package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;

public record ProvidedAttestationResource(
    @NotBlank String format,
    @NotBlank String type
) { }

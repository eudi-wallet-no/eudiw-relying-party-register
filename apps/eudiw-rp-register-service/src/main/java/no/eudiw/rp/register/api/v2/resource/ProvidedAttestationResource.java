package no.eudiw.rp.register.api.v2.resource;

import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.ProvidedAttestation;

public record ProvidedAttestationResource(
    @NotBlank String format,
    @NotBlank String type
) {
    public ProvidedAttestation toDomain() {
        return new ProvidedAttestation(format, type);
    }
}

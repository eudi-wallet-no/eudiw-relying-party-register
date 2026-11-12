package no.eudiw.rp.register.api.v2.resource;

import no.eudiw.rp.register.domain.Law;

public record LawResource(
    String legislativeIdentifier,
    String legalBasis
) {
    public Law toDomain() {
        return new Law(legislativeIdentifier, legalBasis);
    }
}

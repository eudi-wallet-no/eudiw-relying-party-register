package no.eudiw.rp.register.api.v2.endpoints.entities;

public record IntendedUseReplacementResponse(
    IntendedUseResponse revoked,
    IntendedUseResponse current
) { }

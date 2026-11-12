package no.eudiw.rp.register.api.v2.endpoints.entities;

import no.eudiw.rp.register.domain.IntendedUse;

public record IntendedUseReplacementResponse(
    IntendedUseResponse revoked,
    IntendedUseResponse current
) {
    public static IntendedUseReplacementResponse from(IntendedUse revoked, IntendedUse current) {
        return new IntendedUseReplacementResponse(
            IntendedUseResponse.from(revoked),
            IntendedUseResponse.from(current)
        );
    }
}

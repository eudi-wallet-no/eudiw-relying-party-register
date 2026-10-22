package no.eudiw.rp.register.api.v2.endpoints.entities;

import no.eudiw.rp.register.domain.Entitlement;

import java.util.UUID;

public record EntitlementResponse(
    UUID id,
    String entitlement,
    boolean active,
    String displayName,
    String caId
) {
    public static EntitlementResponse from(Entitlement entitlement) {
        return new EntitlementResponse(
            entitlement.getId(),
            entitlement.getEntitlement(),
            entitlement.isActive(),
            entitlement.getDisplayName(),
            entitlement.getCaId()
        );
    }
}

package no.eudiw.rp.register.api.v2.endpoints.entities;

import java.util.UUID;

public record EntitlementResponse(
    UUID id,
    String entitlement,
    boolean active,
    String displayName,
    String caId
) { }

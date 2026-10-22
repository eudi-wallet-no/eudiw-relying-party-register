package no.eudiw.rp.register.api.v2.endpoints.entities;

import no.eudiw.rp.register.domain.Entitlement;

import java.util.List;

public record EntitlementsResponse(List<EntitlementResponse> entitlements) {
    public static EntitlementsResponse from(List<Entitlement> entitlements) {
        return new EntitlementsResponse(entitlements.stream().map(EntitlementResponse::from).toList());
    }
}

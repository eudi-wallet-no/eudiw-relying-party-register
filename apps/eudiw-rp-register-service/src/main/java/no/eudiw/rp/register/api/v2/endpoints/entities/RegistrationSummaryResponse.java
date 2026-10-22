package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.repository.RegistrationSummaryRow;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RegistrationSummaryResponse(
    UUID id,
    String serviceTradeName,
    List<Role> relyingPartyEntitlements,
    long createdMs,
    long lastUpdatedMs
) {
    public static RegistrationSummaryResponse from(List<RegistrationSummaryRow> rows, Map<String, Entitlement> entitlements) {
        RegistrationSummaryRow registration = rows.getFirst();
        List<Role> roles = rows.stream().filter(row -> row.roleId() != null)
            .map(row -> Role.from(row.entitlement(), entitlements)).toList();
        return new RegistrationSummaryResponse(registration.instanceId(), registration.serviceTradeName(), roles,
            registration.createdMs(), registration.lastUpdatedMs());
    }

    public record Role(String displayName) {
        public static Role from(String uri, Map<String, Entitlement> entitlements) {
            if (uri == null) {
                return new Role(null);
            }
            Entitlement entitlement = entitlements.get(uri);
            String name = entitlement == null || entitlement.getDisplayName() == null || entitlement.getDisplayName().isBlank()
                ? uri : entitlement.getDisplayName();
            return new Role(name);
        }
    }
}

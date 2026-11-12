package no.idporten.eudiw.rp.admin.web.resource;

import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient;
import no.idporten.eudiw.rp.admin.integrations.registerservice.V2RegisterClient.Registration;

import java.util.List;
import java.util.UUID;

public record RegistrationSummary(
    UUID id,
    String tradeName,
    List<Role> relyingPartyEntitlements,
    Long createdMs,
    Long lastUpdatedMs
) {
    public static RegistrationSummary from(Registration registration) {
        List<Role> roles = registration.relyingPartyEntitlements() == null ? null
            : registration.relyingPartyEntitlements().stream().map(Role::from).toList();
        return new RegistrationSummary(registration.id(), registration.serviceTradeName(), roles,
            registration.createdMs(), registration.lastUpdatedMs());
    }

    public record Role(String displayName) {
        public static Role from(V2RegisterClient.Role role) {
            return new Role(role == null ? null : role.displayName());
        }
    }
}

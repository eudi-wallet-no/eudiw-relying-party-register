package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.exception.BadRequestException;

import java.util.List;
import java.util.Objects;

public record RelyingPartyEntitlementsRequest(
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEntitlementResource> relyingPartyEntitlements
) {
    public List<RelyingPartyEntitlement> toDomain() {
        if (relyingPartyEntitlements == null) {
            throw new BadRequestException("Entitlements should be empty if none exists");
        }

        if (relyingPartyEntitlements.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("RelyingPartyInstance entitlements must not contain null");
        }

        long distinctEntitlements = relyingPartyEntitlements.stream()
            .map(RelyingPartyEntitlementResource::entitlement)
            .distinct()
            .count();

        if (distinctEntitlements != relyingPartyEntitlements.size()) {
            throw new BadRequestException("RelyingPartyInstance entitlements must be unique");
        }

        return relyingPartyEntitlements.stream()
            .map(RelyingPartyEntitlementResource::toDomain)
            .toList();
    }
}

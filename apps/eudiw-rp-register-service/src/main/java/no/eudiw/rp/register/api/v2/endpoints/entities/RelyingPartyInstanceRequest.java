package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;

import java.util.List;
import java.util.Objects;

public record RelyingPartyInstanceRequest(
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEntitlementResource> relyingPartyEntitlements,
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEaaResource> relyingPartyEaas
) {
    public static RelyingPartyInstanceRequest from(RelyingPartyInstanceResponse relyingPartyInstance) {
        return new RelyingPartyInstanceRequest(
            relyingPartyInstance.relyingPartyEntitlements(),
            relyingPartyInstance.relyingPartyEaas()
        );
    }

    public List<RelyingPartyEntitlement> toDomainRelyingPartyEntitlements() {
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

    public List<RelyingPartyEaa> toDomainRelyingPartyEaas() {
        return relyingPartyEaas.stream().map(RelyingPartyEaaResource::toDomain).toList();
    }

    public void update(
        RelyingPartyInstance relyingPartyInstance,
        List<RelyingPartyEntitlement> relyingPartyEntitlements
    ) {
        relyingPartyInstance.setRelyingPartyEntitlements(relyingPartyEntitlements);
        relyingPartyInstance.setRelyingPartyEaas(toDomainRelyingPartyEaas());
        relyingPartyInstance.setUpdated();
    }
}

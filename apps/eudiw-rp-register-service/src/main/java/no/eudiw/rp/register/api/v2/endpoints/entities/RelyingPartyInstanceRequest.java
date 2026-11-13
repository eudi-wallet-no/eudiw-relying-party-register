package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

import java.util.List;

public record RelyingPartyInstanceRequest(
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEntitlementResource> relyingPartyEntitlements,
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEaaResource> relyingPartyEaas
) {
    public List<RelyingPartyEntitlement> toDomainRelyingPartyEntitlements() {
        return new RelyingPartyEntitlementsRequest(relyingPartyEntitlements).toDomain();
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

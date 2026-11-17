package no.eudiw.rp.register.api.v2.endpoints.entities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.validation.SaneStringConstraint;
import no.idporten.validators.orgnr.Orgnr;

import java.util.List;

public record CreateInstanceRequest(
    @Orgnr @NotBlank String orgno,
    @SaneStringConstraint @NotBlank String serviceTradeName,
    @NotEmpty List<@NotNull @Valid EntitlementRequest> relyingPartyEntitlements
) {
    public record EntitlementRequest(@NotBlank String entitlement) {
        public RelyingPartyEntitlement toDomain() {
            return new RelyingPartyEntitlement(entitlement);
        }
    }
}

package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.validation.SaneStringConstraint;

import java.util.Objects;

public record RelyingPartyServiceSummaryRequest(
    @JsonProperty(required = true) @NotBlank @SaneStringConstraint String serviceTradeName
) {
    public void update(WalletRelyingPartyService relyingPartyService) {
        if (!Objects.equals(relyingPartyService.getServiceTradeName(), serviceTradeName)) {
            relyingPartyService.getRelyingPartyInstances().forEach(RelyingPartyInstance::setUpdated);
        }
        relyingPartyService.setServiceTradeName(serviceTradeName);
        relyingPartyService.setUpdated();
    }
}

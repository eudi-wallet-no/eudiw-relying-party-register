package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;

import java.util.List;

public record RelyingPartyEaasRequest(
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEaaResource> relyingPartyEaas
) {
    public List<RelyingPartyEaa> toDomain() {
        return relyingPartyEaas.stream().map(RelyingPartyEaaResource::toDomain).toList();
    }
}

package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEntitlementResource;

import java.util.List;

public record RelyingPartyInstanceRequest(
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEntitlementResource> relyingPartyEntitlements,
    @JsonProperty(required = true) @NotNull List<@NotNull @Valid RelyingPartyEaaResource> relyingPartyEaas
) { }

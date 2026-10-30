package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegistrationUpdateRequest(
    @JsonProperty(required = true) @NotNull @Valid RelyingPartyRequest relyingParty,
    @JsonProperty(required = true) @NotNull @Valid RelyingPartyServiceRequest relyingPartyService,
    @JsonProperty(required = true) @NotNull @Valid RelyingPartyInstanceRequest relyingPartyInstance
) {
}

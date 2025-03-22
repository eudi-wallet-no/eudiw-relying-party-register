package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartiesResource(
    @Valid
    @NotNull(message = "null_relying_parties")
    @JsonProperty("relying_parties")
    List<RelyingPartyResource> relyingParties
) { }

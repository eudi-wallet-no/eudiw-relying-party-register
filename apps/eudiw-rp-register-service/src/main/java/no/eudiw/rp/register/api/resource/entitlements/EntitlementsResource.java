package no.eudiw.rp.register.api.resource.entitlements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record EntitlementsResource(
    @Valid
    @NotNull(message = "null_entitlements")
    @JsonProperty("entitlements")
    List<EntitlementResource> entitlements
) { }

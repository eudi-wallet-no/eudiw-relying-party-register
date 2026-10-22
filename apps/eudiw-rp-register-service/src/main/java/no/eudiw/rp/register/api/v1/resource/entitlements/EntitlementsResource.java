package no.eudiw.rp.register.api.v1.resource.entitlements;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record EntitlementsResource(
    @Valid
    @NotNull(message = "null_entitlements")
    List<EntitlementResource> entitlements
) { }

package no.idporten.eudiw.rp.admin.web.resource.entitlement;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntitlementsResource(
    @Valid
    @NotNull(message = "null_entitlements")
    @JsonProperty(value = "entitlements", required = true)
    List<EntitlementResource> entitlements
) { }

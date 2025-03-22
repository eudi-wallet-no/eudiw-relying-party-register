package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import no.eudiw.rp.register.validation.SaneStringConstraint;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RelyingPartyEntitlementResource(

    @SaneStringConstraint(message = "unsane_entitlement")
    @NotBlank(message = "blank_entitlement")
    @JsonProperty("entitlement")
    String entitlement
) { }

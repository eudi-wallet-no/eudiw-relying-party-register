package no.eudiw.rp.register.api.resource.entitlements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.With;
import no.eudiw.rp.register.validation.SaneStringConstraint;

@With
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CreateEntitlementResource(

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "entitlement", required = true)
    String entitlement,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "displayName", required = true)
    String displayName,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "ca_id", required = true)
    String caId
) { }

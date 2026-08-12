package no.eudiw.rp.register.api.resource.entitlements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;

@With
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EditEntitlementResource(

    @JsonProperty(value = "active", required = true)
    boolean active
) { }

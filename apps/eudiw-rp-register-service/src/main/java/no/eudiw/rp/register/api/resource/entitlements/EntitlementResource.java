package no.eudiw.rp.register.api.resource.entitlements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.With;
import no.eudiw.rp.register.validation.SaneStringConstraint;
import java.util.UUID;

@With
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EntitlementResource(

    @NotNull(message = "null_id")
    @JsonProperty("id")
    UUID id,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "entitlement", required = true)
    String entitlement,

    @JsonProperty(value = "active", required = true)
    boolean active,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "display_name", required = true)
    String displayName,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "ca_id", required = true)
    String caId
) { }

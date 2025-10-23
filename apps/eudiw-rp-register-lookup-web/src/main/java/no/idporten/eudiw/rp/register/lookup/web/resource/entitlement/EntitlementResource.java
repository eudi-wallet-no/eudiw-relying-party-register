package no.idporten.eudiw.rp.register.lookup.web.resource.entitlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringConstraint;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntitlementResource(
    @NotNull(message = "null_id")
    @JsonProperty(value = "id", required = true)
    UUID id,

    @SaneStringConstraint
    @NotBlank(message = "blank_entitlement")
    @JsonProperty(value = "entitlement", required = true)
    String entitlement,

    @SaneStringConstraint
    @NotBlank(message = "blank_display_name")
    @JsonProperty(value = "display_name", required = true)
    String displayName,

    @JsonProperty(value = "active", required = true)
    boolean active
) { }

package no.idporten.eudiw.rp.admin.web.resource.entitlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntitlementResource(
    @NotNull(message = "null_id")
    @JsonProperty(value = "id", required = true)
    UUID id,

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "entitlement", required = true)
    String entitlement,

    @JsonProperty(value = "active", required = true)
    boolean active
) { }

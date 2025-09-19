package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RequestAuthorizationDetails(
    @NotBlank
    @JsonProperty(value = "type", required = true)
    String type,
    @NotBlank
    @JsonProperty(value = "resource", required = true)
    String resource,
    @JsonProperty("allow_multiple_organizations")
    boolean allowMultipleOrganizations
) { }

package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenReportee;

import java.util.List;

public record ResponseAuthorizationDetails(
    @NotBlank
    @JsonProperty("type")
    String type,
    @NotBlank
    @JsonProperty("resource")
    String resource,
    @NotBlank
    @JsonProperty("resource_name")
    String resourceName,
    @NotNull
    @JsonProperty("reportees")
    List<AnsattportenReportee> reportees
) { }

package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface AuthorizationDetails {

    @JsonProperty("type")
    @NotBlank
    String getType();

    interface Response extends AuthorizationDetails {

        @NotNull
        List<@Valid Reportee> getReportees();

        boolean canMatchRequest(AuthorizationDetails.Request request);

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Reportee(
            @NotBlank
            @JsonProperty(value = "ID", required = true)
            String orgno,
            @JsonProperty("Name")
            String name
        ) { }
    }

    interface Request extends AuthorizationDetails { }
}

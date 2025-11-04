package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@Validated
@ConfigurationProperties(prefix = "eudiw-admin-web.security.ansattporten")
public class AnsattportenProperties {
    @Valid
    private List<RequestAuthorizationDetails> requestAuthorizationDetails = List.of();

    private boolean allowSyntheticReportee = false;

    @Getter
    @Setter
    @EqualsAndHashCode
    public static class RequestAuthorizationDetails {
        @NotBlank
        @JsonProperty(value = "type", required = true)
        private String type;
        @NotBlank
        @JsonProperty(value = "resource", required = true)
        private String resource;
        @JsonProperty(value = "representation_is_required", required = true)
        private boolean representationIsRequired = true;
    }
}

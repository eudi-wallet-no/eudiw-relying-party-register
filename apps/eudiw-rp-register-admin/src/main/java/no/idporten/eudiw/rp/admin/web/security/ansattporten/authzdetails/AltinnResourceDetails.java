package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AltinnResourceDetails implements AuthorizationDetails {

    public static final String TYPE_VALUE = "ansattporten:altinn:resource";

    private final String type = TYPE_VALUE;

    @NotBlank
    protected String resource;

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends AltinnResourceDetails implements AuthorizationDetails.Response {

        @JsonProperty("authorized_parties")
        private List<AuthorizedParties> authorizedParties = List.of();

        @Override
        public boolean canMatchRequest(AuthorizationDetails.Request other) {
            return other instanceof AltinnResourceDetails altinnServiceDetailsRequest
                       && Objects.equals(this.resource, altinnServiceDetailsRequest.resource);
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class Request
        extends AltinnResourceDetails implements AuthorizationDetails.Request {
        @JsonProperty(value = "representation_is_required", required = true)
        private final boolean representationIsRequired = true;
    }
}

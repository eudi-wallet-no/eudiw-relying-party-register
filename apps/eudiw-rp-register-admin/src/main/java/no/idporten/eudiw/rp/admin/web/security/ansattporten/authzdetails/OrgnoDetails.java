package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class OrgnoDetails implements AuthorizationDetails {
    public static final String TYPE_VALUE = "ansattporten:orgno";

    private final String type = TYPE_VALUE;

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends OrgnoDetails implements AuthorizationDetails.Response {

        @JsonProperty("authorized_parties")
        private List<AuthorizationDetails.Response.AuthorizedParties> authorizedParties;


        @JsonIgnore
        public List<AuthorizedParties> getAuthorizedParties() {
            return authorizedParties != null ? authorizedParties : List.of();
        }

        @Override
        public boolean canMatchRequest(AuthorizationDetails.Request request) {
            return request instanceof OrgnoDetails;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class Request
        extends OrgnoDetails implements AuthorizationDetails.Request {
        @JsonProperty(value = "representation_is_required", required = true)
        private final boolean representationIsRequired = true;
    }
}

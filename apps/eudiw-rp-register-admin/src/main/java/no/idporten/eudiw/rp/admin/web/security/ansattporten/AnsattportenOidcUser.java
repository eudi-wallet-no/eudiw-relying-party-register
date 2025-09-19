package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import no.idporten.eudiw.rp.admin.web.security.BaseOidcUser;
import no.idporten.eudiw.rp.admin.web.security.OAuth2Constants;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.ResponseAuthorizationDetails;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public class AnsattportenOidcUser extends BaseOidcUser {

    private List<AnsattportenReportee> reportees;

    public AnsattportenOidcUser(Collection<? extends GrantedAuthority> authorities,
                                OidcIdToken idToken,
                                OidcUserInfo userInfo) {
        super(authorities, idToken, userInfo);

        ResponseAuthorizationDetails authzDetails = getAuthzDetailsFromIdToken(idToken);
        this.reportees = authzDetails.reportees();
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
    @Override
    public boolean hasPrivilegedAccessTo(String orgno) {
        return reportees.stream()
                        .anyMatch(reportee -> Objects.equals(reportee.orgno(), orgno));
    }

    private ResponseAuthorizationDetails getAuthzDetailsFromIdToken(OidcIdToken idToken) {
        if (!idToken.hasClaim(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)) {
            throw new InsufficientAuthenticationException("Authz details claim missing");
        }
        List<Map<String, Object>> authzDetailsClaim =
            idToken.getClaim(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER);

        List<ResponseAuthorizationDetails> authzDetails;
        try {
            ObjectMapper om = new ObjectMapper();
            authzDetails =
                authzDetailsClaim
                    .stream()
                    .map(map -> om.convertValue(map, ResponseAuthorizationDetails.class))
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new InsufficientAuthenticationException("Invalid authz details claims", e);
        }
        // authzDetails is expected to contain exactly one element.
        if (authzDetails.isEmpty()) {
            throw new InsufficientAuthenticationException("Authz details claims found but empty");
        }
        return authzDetails.getFirst();
    }
}

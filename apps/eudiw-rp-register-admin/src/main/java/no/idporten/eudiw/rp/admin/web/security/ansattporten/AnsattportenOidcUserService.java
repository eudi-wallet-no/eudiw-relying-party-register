package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import no.idporten.eudiw.rp.admin.web.security.OAuth2Constants;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.ResponseAuthorizationDetails;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceOidcUser;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceReportee;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class AnsattportenOidcUserService extends OidcUserService {

    private final EnhetsregisteretService enhetsregisteretService;

    private ResponseAuthorizationDetails getAndValidateAuthzDetailsClaim(OidcIdToken idToken) {
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

    private SelfServiceReportee toSelfServiceReporteeWithSectorInfo(
        ResponseAuthorizationDetails.Reportee reportee) {
        boolean isPublicSector = enhetsregisteretService.getPublicSectorForOrgno(reportee.orgno());
        return new SelfServiceReportee(
            reportee.orgno(),
            reportee.name(),
            isPublicSector);
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
        throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        ResponseAuthorizationDetails responseAuthzDetails =
            getAndValidateAuthzDetailsClaim(oidcUser.getIdToken());

        List<SelfServiceReportee> reportees =
            responseAuthzDetails.reportees()
                                .stream()
                                .map(this::toSelfServiceReporteeWithSectorInfo)
                                .toList();

        return new SelfServiceOidcUser(oidcUser.getAuthorities(),
                                       oidcUser.getIdToken(),
                                       oidcUser.getUserInfo(),
                                       reportees);
    }
}

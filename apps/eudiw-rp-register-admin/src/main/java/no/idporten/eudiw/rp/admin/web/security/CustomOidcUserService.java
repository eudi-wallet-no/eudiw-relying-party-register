package no.idporten.eudiw.rp.admin.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.ResponseAuthorizationDetails;
import no.idporten.eudiw.rp.admin.web.security.entraid.EntraIdProperties;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.AdminOidcUser;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceOidcUser;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.SelfServiceReportee;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final EntraIdProperties entraIdProperties;
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
        boolean isPublicSector = false;
        try {
            isPublicSector = enhetsregisteretService.getPublicSectorForOrgno(reportee.orgno());
        } catch (Exception e) {
            log.warn("Failed to get public sector info from Enhetsregisteret (defaulting FALSE)", e);
        }
        return new SelfServiceReportee(
            reportee.orgno(),
            reportee.name(),
            isPublicSector);
    }

    private OidcUser mapAnsattportenUser(OidcUser oidcUser) {
        ResponseAuthorizationDetails responseAuthzDetails =
            getAndValidateAuthzDetailsClaim(oidcUser.getIdToken());

        List<SelfServiceReportee> reportees =
            responseAuthzDetails.reportees()
                                .stream()
                                .map(this::toSelfServiceReporteeWithSectorInfo)
                                .toList();
        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
        authorities.addAll(reportees);

        return new SelfServiceOidcUser(authorities,
                                       oidcUser.getIdToken(),
                                       oidcUser.getUserInfo(),
                                       reportees);
    }

    private OidcUser mapEntraIdUser(OidcUser oidcUser) throws OAuth2AuthenticationException {

        boolean hasWriteAccess =
            oidcUser.hasClaim("groups")
                && oidcUser.getClaimAsStringList("groups")
                           .contains(entraIdProperties.writeAccess());
        if (!hasWriteAccess) {
            throw new AuthenticationException(OAuth2ErrorCodes.INSUFFICIENT_SCOPE,
                "Insufficient (or missing) access groups claims");
        }

        Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
        mapped.add(new SimpleGrantedAuthority(toAuthority(entraIdProperties.writeAccess())));
        mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return new AdminOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    public static String toAuthority(String groupId) {
        return "GROUP_" + groupId;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
        throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        return
            switch (userRequest.getClientRegistration().getRegistrationId()) {
                case "ansattporten" -> mapAnsattportenUser(oidcUser);
                case "entra" -> mapEntraIdUser(oidcUser);
                default -> throw new AuthenticationException(
                    OAuth2ErrorCodes.INVALID_CLIENT, "OidcUserRequest from unrecognized registration ID");
            };
    }
}

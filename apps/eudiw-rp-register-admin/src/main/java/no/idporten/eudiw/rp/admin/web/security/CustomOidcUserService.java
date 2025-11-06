package no.idporten.eudiw.rp.admin.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import no.idporten.eudiw.rp.admin.service.syntheticreportees.SyntheticReporteeProvider;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetails;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetailsMapper;
import no.idporten.eudiw.rp.admin.web.security.entraid.EntraIdProperties;
import no.idporten.eudiw.rp.admin.web.security.exception.AuthenticationException;
import no.idporten.eudiw.rp.admin.web.security.exception.InvalidClaimsException;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.OidcUserWithCustomName;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final EntraIdProperties entraIdProperties;
    private final AnsattportenProperties ansattportenProperties;
    private final EnhetsregisteretService enhetsregisteretService;
    private final SyntheticReporteeProvider syntheticReporteeProvider;
    private final AuthorizationDetailsMapper authorizationDetailsMapper;

    private List<AuthorizationDetails.Response> parseAuthorizationDetailsFromIdToken(
        OidcIdToken idToken) {
        if (!idToken.hasClaim(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)) {
            throw new InvalidClaimsException(
                "authorization_details claim expected but missing");
        }
        try {
            List<Map<String, Object>> authzDetailsClaim =
                idToken.getClaim(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER);
            return authzDetailsClaim
                .stream()
                .map(authorizationDetailsMapper::asResponse)
                .toList();
        } catch (Exception e) {
            throw new InvalidClaimsException("Ill-formed authz details claims", e);
        }
    }

    private boolean isValidAuthorizationDetailsWithReportees(
        AuthorizationDetails.Response authorizationDetails) {
        boolean hasRecognizedTypeAndResource =
            ansattportenProperties.getRequestAuthorizationDetails()
                                  .stream()
                                  .anyMatch(authorizationDetails::canMatchRequest);
        boolean hasReportee =
            authorizationDetails.getReportees() != null
                && !authorizationDetails.getReportees().isEmpty();
        return hasRecognizedTypeAndResource && hasReportee;
    }

    private AuthorizationDetails.Response.Reportee
    getAndValidateReporteeAuthorityClaim(OidcIdToken idToken) {
        List<AuthorizationDetails.Response> authzDetails =
            parseAuthorizationDetailsFromIdToken(idToken);
        if (authzDetails.isEmpty()) {
            throw new InvalidClaimsException("authorization_details found but empty");
        }
        AuthorizationDetails.Response firstValidAuthzDetailsWithReportees =
            authzDetails.stream()
                        .filter(this::isValidAuthorizationDetailsWithReportees)
                        .findAny()
                        .orElseThrow(
                            () -> new InvalidClaimsException(
                                "Found no valid authorization_details with reportees"));
        return firstValidAuthzDetailsWithReportees.getReportees().getFirst();
    }

    private ReporteeAuthority getReporteeAuthorityForOidcUser(OidcUser oidcUser) {
        if (oidcUser.getIdToken().hasClaim(
            OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)) {

            AuthorizationDetails.Response.Reportee reportee =
                getAndValidateReporteeAuthorityClaim(oidcUser.getIdToken());
            boolean isPublicSector = false;
            try {
                isPublicSector = enhetsregisteretService.getPublicSectorForOrgno(reportee.orgno());
            } catch (Exception e) {
                log.warn("Failed to get public sector info from Enhetsregisteret (defaulting FALSE)", e);
            }
            return new ReporteeAuthority(
                reportee.orgno(), reportee.name(), isPublicSector);
        }
        if (ansattportenProperties.isAllowSyntheticReportee()) {
            String userId = oidcUser.getIdToken().getClaim("pid");
            return syntheticReporteeProvider.getSyntheticReporteeAuthority(userId);
        }
        throw new InvalidClaimsException(
            "authorization_details claim missing, and synthetic reportees NOT allowed");
    }

    private OidcUser mapAnsattportenUser(OidcUser oidcUser) throws OAuth2AuthenticationException {

        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
        ReporteeAuthority reportee = getReporteeAuthorityForOidcUser(oidcUser);
        authorities.add(reportee);

        String username = oidcUser.getClaim(oidcUser.hasClaim("name") ? "name" : "pid");
        String name = username != null
                          ? "%s - %s".formatted(username, reportee.name())
                          : reportee.name();
        return new OidcUserWithCustomName(authorities,
                                          oidcUser.getIdToken(),
                                          oidcUser.getUserInfo(),
                                          name);
    }

    private OidcUser mapEntraIdUser(OidcUser oidcUser) throws OAuth2AuthenticationException {

        boolean hasWriteAccess =
            oidcUser.hasClaim("groups")
                && oidcUser.getClaimAsStringList("groups")
                           .contains(entraIdProperties.writeAccess());
        if (!hasWriteAccess) {
            throw new InvalidClaimsException("Insufficient (or missing) access groups claims");
        }

        Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
        mapped.add(new SimpleGrantedAuthority(toAuthority(entraIdProperties.writeAccess())));
        mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return new DefaultOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo(), "preferred_username");
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

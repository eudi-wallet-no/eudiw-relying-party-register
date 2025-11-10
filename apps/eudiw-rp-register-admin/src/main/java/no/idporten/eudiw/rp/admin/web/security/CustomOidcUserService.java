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
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.*;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final EntraIdProperties entraIdProperties;
    private final AnsattportenProperties ansattportenProperties;
    private final EnhetsregisteretService enhetsregisteretService;
    private final SyntheticReporteeProvider syntheticReporteeProvider;
    private final AuthorizationDetailsMapper authorizationDetailsMapper;

    private List<AuthorizationDetails.Response> getAuthorizationDetailsForOidcUser(
        OidcUser oidcUser) {
        if (!oidcUser.hasClaim(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER)) {
            throw new InvalidClaimsException(
                "authorization_details claim expected but missing");
        }
        try {
            List<Map<String, Object>> authzDetailsClaim =
                oidcUser.getClaim(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER);
            return authzDetailsClaim
                .stream()
                .map(authorizationDetailsMapper::asResponse)
                .toList();
        } catch (Exception e) {
            throw new InvalidClaimsException("Ill-formed authz details claims", e);
        }
    }

    private Predicate<AuthorizationDetails.Response> getAuthorizationDetailsTypeValidator(
        boolean isEntraIdUser) {
        List<AuthorizationDetails.Request> validRequests =
            isEntraIdUser
                ? ansattportenProperties.getEntraIdRequestAuthorizationDetails()
                : ansattportenProperties.getRequestAuthorizationDetails();
        return authzDetails ->
                   validRequests.stream().anyMatch(authzDetails::canMatchRequest);
    }

    private AuthorizationDetails.Response.Reportee getAndValidateReporteeClaim(
        OidcUser oidcUser, boolean isEntraIdUser) {
        List<AuthorizationDetails.Response> authorizationDetails =
            getAuthorizationDetailsForOidcUser(oidcUser);

        Predicate<AuthorizationDetails.Response> isAcceptedauthorizationDetailsType =
            getAuthorizationDetailsTypeValidator(isEntraIdUser);

        AuthorizationDetails.Response firstValidAuthzDetailsWithReportees =
            authorizationDetails
                .stream()
                .filter(isAcceptedauthorizationDetailsType)
                .filter(authorizationDetail -> !authorizationDetail.getReportees().isEmpty())
                .findAny()
                .orElseThrow(
                    () -> new InvalidClaimsException(
                        "Found no valid authorization_details with reportees"));
        return firstValidAuthzDetailsWithReportees.getReportees().getFirst();
    }

    private ReporteeAuthority getReporteeAuthorityForOidcUser(OidcUser oidcUser, boolean isEntraIdUser) {
        if (oidcUser.hasClaim(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER)) {

            AuthorizationDetails.Response.Reportee reportee =
                getAndValidateReporteeClaim(oidcUser, isEntraIdUser);
            String name = reportee.name() != null ? reportee.name() : reportee.orgno();
            boolean isPublicSector = false;
            try {
                EnhetsregisteretService.EnhetsregisteretResponse response =
                    enhetsregisteretService.queryOrgno(reportee.orgno());
                name = response.name();
                isPublicSector = response.publicSector();
            } catch (Exception e) {
                log.warn("Failed to get name/sector info from Enhetsregisteret "
                             + "(using name=orgno, publicSector=FALSE)", e);
            }
            return new ReporteeAuthority(reportee.orgno(), name, isPublicSector);
        }
        if (isEntraIdUser) {
            throw new InvalidClaimsException(
                "Ansattporten EntraID user without authorization_details");
        }
        if (!ansattportenProperties.isAllowSyntheticReportee()) {
            throw new InvalidClaimsException(
                "authorization_details claim missing, and synthetic reportees NOT allowed");
        }
        String userId = oidcUser.getClaim("pid");
        return syntheticReporteeProvider.getSyntheticReporteeAuthority(userId);
    }

    private OidcUser mapAnsattportenUser(OidcUser oidcUser) throws OAuth2AuthenticationException {

        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());

        boolean isEntraIdUser =
            oidcUser.hasClaim(AuthConstants.ACR_PARAMETER)
                && oidcUser.getClaimAsString(AuthConstants.ACR_PARAMETER)
                           .contains(AuthConstants.ACR_ENTRAID_VALUE);
        if (isEntraIdUser && !ansattportenProperties.isAllowEntraId()) {
            throw new AuthenticationException(
                OAuth2ErrorCodes.ACCESS_DENIED, "Ansattporten EntraID not accepted");
        }

        ReporteeAuthority reportee = getReporteeAuthorityForOidcUser(oidcUser, isEntraIdUser);
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

package no.idporten.eudiw.rp.admin.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.ResponseAuthorizationDetails;
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
            throw new InvalidClaimsException("Authz details claim missing");
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
            throw new InvalidClaimsException("Ill-formed authz details claims", e);
        }
        if (authzDetails.size() != 1) {
            throw new InvalidClaimsException(
                "Expected exactly 1 authorization_details in token, found %s"
                    .formatted(authzDetails.size()));
        }
        return authzDetails.getFirst();
    }

    private ReporteeAuthority toSelfServiceReporteeWithSectorInfo(
        ResponseAuthorizationDetails.Reportee reportee) {
        boolean isPublicSector = false;
        try {
            isPublicSector = enhetsregisteretService.getPublicSectorForOrgno(reportee.orgno());
        } catch (Exception e) {
            log.warn("Failed to get public sector info from Enhetsregisteret (defaulting FALSE)", e);
        }
        return new ReporteeAuthority(
            reportee.orgno(),
            reportee.name(),
            isPublicSector);
    }

    private OidcUser mapAnsattportenUser(OidcUser oidcUser) throws OAuth2AuthenticationException {
        ResponseAuthorizationDetails responseAuthzDetails =
            getAndValidateAuthzDetailsClaim(oidcUser.getIdToken());

        if (responseAuthzDetails.reportees().size() != 1) {
            throw new InvalidClaimsException(
                "Expected exactly 1 reportee, found %s".formatted(
                    responseAuthzDetails.reportees().size()));
        }

        ReporteeAuthority reportee = toSelfServiceReporteeWithSectorInfo(
            responseAuthzDetails.reportees().getFirst());
        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
        authorities.add(reportee);

        String name = "%s - %s".formatted(oidcUser.getClaim("name"), reportee.name());
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

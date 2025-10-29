package no.idporten.eudiw.rp.admin.web.security;

import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

import java.util.Objects;

public class UserAuthorityService {

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public boolean userHasAdminAuthority() {
        try {
            return getAuthentication()
                       .getAuthorities()
                       .stream()
                       .map(GrantedAuthority::getAuthority)
                       .anyMatch("ROLE_ADMIN"::equals);
        } catch (Exception _) {
            return false;
        }
    }

    public boolean userHasAccessTo(String orgno) {
        return userHasAdminAuthority()
            || Objects.equals(getReporteeAuthority().orgno(), orgno);
    }

    public void assertUserHasAccessTo(String orgno) {
        if (!userHasAccessTo(orgno)) {
            throw new AuthenticationException(
                OAuth2ErrorCodes.ACCESS_DENIED,
                "User does not have access to RP with orgno=%s".formatted(orgno));
        }
    }

    public ReporteeAuthority getReporteeAuthority() {
        return getAuthentication()
                   .getAuthorities()
                   .stream()
                   .filter(ReporteeAuthority.class::isInstance)
                   .map(authority -> (ReporteeAuthority) authority)
                   .findFirst()
                   .orElseThrow(() -> new AuthenticationException(
                       OAuth2ErrorCodes.INSUFFICIENT_SCOPE,
                       "No reportee authority found for user"));
    }
}

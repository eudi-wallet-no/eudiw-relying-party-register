package no.idporten.eudiw.rp.admin.web.security;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.security.exception.InsufficientAuthorityException;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.AuthorizedPartyAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
            || Objects.equals(getAuthorizedPartyAuthority().orgno(), orgno);
    }

    public void assertUserHasAccessTo(String orgno) {
        if (!userHasAccessTo(orgno)) {
            throw new InsufficientAuthorityException(
                "User does not have access to RP with orgno=%s".formatted(orgno));
        }
    }

    public AuthorizedPartyAuthority getAuthorizedPartyAuthority() {
        return getAuthentication()
                   .getAuthorities()
                   .stream()
                   .filter(AuthorizedPartyAuthority.class::isInstance)
                   .map(authority -> (AuthorizedPartyAuthority) authority)
                   .findFirst()
                   .orElseThrow(() -> new InsufficientAuthorityException(
                       "No authorized party found for user"));
    }

    public boolean isLegalEditResourceForRelyingParty(
        EditRelyingPartyResource editResource, RelyingPartyResource relyingPartyResource) {

        if (userHasAdminAuthority()) {
            return true;
        }

        boolean activeStatusPreserved = editResource.active() == relyingPartyResource.active();

        Set<String> existingEntitlements =
            relyingPartyResource.relyingPartyEntitlements()
                                .stream()
                                .map(RelyingPartyEntitlementResource::entitlement)
                                .collect(Collectors.toSet());
        Set<String> requestedEntitlements =
            editResource.relyingPartyEntitlements()
                        .stream()
                        .map(RelyingPartyEntitlementResource::entitlement)
                        .collect(Collectors.toSet());

        boolean entitlementValuesPreserved = existingEntitlements.equals(requestedEntitlements);

        return activeStatusPreserved && entitlementValuesPreserved;
    }

    public void assertIsLegalEditResourceForRelyingParty(
        EditRelyingPartyResource editResource, RelyingPartyResource relyingPartyResource) {
        if (!isLegalEditResourceForRelyingParty(editResource, relyingPartyResource)) {
            throw new InsufficientAuthorityException("Unauthorized RP edit attempt");
        }
    }
}

package no.idporten.eudiw.rp.admin.web.security;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // used in Spring Security
public class AuthorizationService {
    private final RelyingPartiesService relyingPartiesService;

    private boolean isAuthenticated() {
        return SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .isAuthenticated();
    }

    private BaseOidcUser getBaseOidcUserPrincipal() {
        Object principal =
            SecurityContextHolder.getContext()
                                 .getAuthentication()
                                 .getPrincipal();
        if (principal instanceof BaseOidcUser baseOidcUserPrincipal) {
            return baseOidcUserPrincipal;
        }
        throw new AuthorizationDeniedException(
            "Unexpected AuthenticationPrincipal type (expected BaseOidcUser, got %s)"
                .formatted(principal.getClass().getName()));
    }

    public boolean userHasPrivilegedAccessTo(String orgno) {
        return isAuthenticated() && getBaseOidcUserPrincipal().hasPrivilegedAccessTo(orgno);
    }
    public boolean userHasPrivilegedAccessTo(UUID id) {
        if (!isAuthenticated()) {
            return false;
        }
        BaseOidcUser oidcUser = getBaseOidcUserPrincipal();
        try {
            String orgno = relyingPartiesService.get(id).orgno();
            return oidcUser.hasPrivilegedAccessTo(orgno);
        } catch (AdminServiceException _) {
            throw new AuthorizationDeniedException(
                "Application-level exception trying to determine authorization");
        }
    }
}

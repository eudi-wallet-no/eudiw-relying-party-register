package no.idporten.eudiw.rp.admin.web.security.entraid;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.AdminOidcUser;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class EntraIdOidcUserService extends OidcUserService {

    private final EntraIdProperties entraIdProperties;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        boolean hasWriteAccess =
            oidcUser.hasClaim("groups")
                && oidcUser.getClaimAsStringList("groups")
                           .contains(entraIdProperties.writeAccess());
        if (!hasWriteAccess) {
            throw new InsufficientAuthenticationException(
                "Insufficient access groups (or groups claims missing)");
        }

        Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
        mapped.add(new SimpleGrantedAuthority(toAuthority(entraIdProperties.writeAccess())));
        mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return new AdminOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    public static String toAuthority(String groupId) {
        return "GROUP_" + groupId;
    }
}

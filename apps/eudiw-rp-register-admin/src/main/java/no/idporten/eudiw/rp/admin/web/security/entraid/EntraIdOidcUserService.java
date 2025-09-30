package no.idporten.eudiw.rp.admin.web.security.entraid;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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

        List<String> groups =
            Objects.requireNonNullElseGet(oidcUser.getClaimAsStringList("groups"),
                                          List::of);

        boolean hasAccess = groups.contains(entraIdProperties.writeAccess());
        if (!hasAccess) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("access_denied"), "Du er ikke i riktig access group");
        }

        Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
        mapped.add(new SimpleGrantedAuthority(toAuthority(entraIdProperties.writeAccess())));

        return new EntraIdOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    public static String toAuthority(String groupId) {
        return "GROUP_" + groupId;
    }
}

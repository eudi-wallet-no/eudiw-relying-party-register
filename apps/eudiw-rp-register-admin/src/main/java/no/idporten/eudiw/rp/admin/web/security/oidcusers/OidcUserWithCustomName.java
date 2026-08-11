package no.idporten.eudiw.rp.admin.web.security.oidcusers;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

@Getter
public class OidcUserWithCustomName extends DefaultOidcUser {

    private final String name;

    public OidcUserWithCustomName(Collection<? extends GrantedAuthority> authorities,
                                  OidcIdToken idToken,
                                  OidcUserInfo userInfo,
                                  String name) {
        super(authorities, idToken, userInfo);
        this.name = name;
    }
}

package no.idporten.eudiw.rp.admin.web.security.entraid;

import no.idporten.eudiw.rp.admin.web.security.BaseOidcUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Collection;

public class EntraIdOidcUser extends BaseOidcUser {
    public EntraIdOidcUser(Collection<? extends GrantedAuthority> authorities,
                           OidcIdToken idToken,
                           OidcUserInfo userInfo) {
        super(authorities, idToken, userInfo);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
    @Override
    public boolean hasPrivilegedAccessTo(String orgno) {
        return true;
    }
}

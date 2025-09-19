package no.idporten.eudiw.rp.admin.web.security;

import lombok.Getter;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenOidcUser;
import no.idporten.eudiw.rp.admin.web.security.entraid.EntraIdOidcUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;
import java.util.Objects;

public abstract class BaseOidcUser extends DefaultOidcUser {

    public BaseOidcUser(Collection<? extends GrantedAuthority> authorities,
                        OidcIdToken idToken,
                        OidcUserInfo userInfo) {
        super(authorities, idToken, userInfo);
        this.name = Objects.requireNonNullElseGet(
            idToken.getClaimAsString("preferred_username"),
            () -> idToken.getClaimAsString("name"));
    }

    @Getter
    protected String name;

    public abstract boolean isAdmin();
    public abstract boolean hasPrivilegedAccessTo(String orgno);

    public AnsattportenOidcUser toAnsattportenOidcUser() {
        return (AnsattportenOidcUser) this;
    }
    public EntraIdOidcUser toEntraIdOidcUser() {
        return (EntraIdOidcUser) this;
    }
}

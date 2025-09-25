package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class AnsattportenOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        return new AnsattportenOidcUser(oidcUser.getAuthorities(),
                                        oidcUser.getIdToken(),
                                        oidcUser.getUserInfo());
    }

}

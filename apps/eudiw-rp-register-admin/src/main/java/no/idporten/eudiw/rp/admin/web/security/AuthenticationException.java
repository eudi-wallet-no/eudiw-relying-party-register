package no.idporten.eudiw.rp.admin.web.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class AuthenticationException extends OAuth2AuthenticationException {
    public AuthenticationException(String errorCode, String message) {
        super(new OAuth2Error(errorCode, message, null));
    }
}

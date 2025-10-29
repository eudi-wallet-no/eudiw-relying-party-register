package no.idporten.eudiw.rp.admin.web.security.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class AuthenticationException extends OAuth2AuthenticationException {
    public AuthenticationException(String errorCode, String message) {
        super(new OAuth2Error(errorCode, message, null));
    }
    public AuthenticationException(String errorCode, String message, Throwable e) {
        super(new OAuth2Error(errorCode, message, null), e);
    }
}

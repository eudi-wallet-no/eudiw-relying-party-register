package no.idporten.eudiw.rp.admin.web.security.exception;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class InsufficientAuthorityException extends AuthenticationException {
    public InsufficientAuthorityException(String message) {
        super(OAuth2ErrorCodes.ACCESS_DENIED, message);
    }
}

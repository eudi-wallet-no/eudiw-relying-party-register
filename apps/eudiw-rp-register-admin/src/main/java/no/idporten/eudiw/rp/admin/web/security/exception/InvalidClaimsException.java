package no.idporten.eudiw.rp.admin.web.security.exception;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class InvalidClaimsException extends AuthenticationException {
    public InvalidClaimsException(String message) {
        super(OAuth2ErrorCodes.INVALID_REQUEST, message);
    }
    public InvalidClaimsException(String message, Throwable e) {
        super(OAuth2ErrorCodes.INVALID_REQUEST, message, e);
    }
}

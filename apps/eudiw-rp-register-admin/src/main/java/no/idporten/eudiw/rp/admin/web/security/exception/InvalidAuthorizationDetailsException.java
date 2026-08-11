package no.idporten.eudiw.rp.admin.web.security.exception;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class InvalidAuthorizationDetailsException extends CustomAuthenticationException {
    public InvalidAuthorizationDetailsException(String message) {
        super(OAuth2ErrorCodes.INVALID_REQUEST, ErrorCodes.INVALID_AUTHORIZATION_DETAILS, message);
    }
    public InvalidAuthorizationDetailsException(String message, Throwable e) {
        super(OAuth2ErrorCodes.INVALID_REQUEST, ErrorCodes.INVALID_AUTHORIZATION_DETAILS, message, e);
    }
}

package no.idporten.eudiw.rp.admin.web.security.exception;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class InsufficientAuthorityException extends CustomAuthenticationException {
    public InsufficientAuthorityException(String message) {
        super(OAuth2ErrorCodes.ACCESS_DENIED, ErrorCodes.INSUFFICIENT_AUTHORITY, message);
    }
}

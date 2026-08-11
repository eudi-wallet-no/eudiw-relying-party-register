package no.idporten.eudiw.rp.admin.web.security.exception;

import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@Getter
public class CustomAuthenticationException extends OAuth2AuthenticationException {
    private final String errorCode;

    public CustomAuthenticationException(String oauthErrorCode, String errorCode, String message) {
        super(new OAuth2Error(oauthErrorCode, message, null));
        this.errorCode = errorCode;
    }
    public CustomAuthenticationException(String oauthErrorCode, String errorCode, String message, Throwable e) {
        super(new OAuth2Error(oauthErrorCode, message, null), e);
        this.errorCode = errorCode;
    }
}

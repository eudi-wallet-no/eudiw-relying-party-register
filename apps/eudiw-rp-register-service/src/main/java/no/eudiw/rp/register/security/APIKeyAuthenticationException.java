package no.eudiw.rp.register.security;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class APIKeyAuthenticationException extends AuthenticationException {

    private final String error;
    private final HttpStatus httpStatus;

    APIKeyAuthenticationException(String msg) {
        super(msg);
        this.httpStatus = HttpStatus.UNAUTHORIZED;
        this.error = "invalid_request";
    }

    public String getErrorDescription() {
        return super.getMessage();
    }
}

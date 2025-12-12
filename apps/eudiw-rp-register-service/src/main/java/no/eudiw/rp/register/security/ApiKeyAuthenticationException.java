package no.eudiw.rp.register.security;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class ApiKeyAuthenticationException extends AuthenticationException {

    private final String error;
    private final HttpStatus httpStatus;

    ApiKeyAuthenticationException(String msg) {
        super(msg);
        this.httpStatus = HttpStatus.UNAUTHORIZED;
        this.error = "invalid_request";
    }

    public String getErrorDescription() {
        return super.getMessage();
    }
}

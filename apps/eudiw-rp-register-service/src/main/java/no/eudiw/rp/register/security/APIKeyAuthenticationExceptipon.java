package no.eudiw.rp.register.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class APIKeyAuthenticationExceptipon extends AuthenticationException {

    private final String error;
    private HttpStatus httpStatus;


    APIKeyAuthenticationExceptipon(String msg) {
        super(msg);
        this.httpStatus = HttpStatus.UNAUTHORIZED;
        this.error = "invalid_request";
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return super.getMessage();
    }

}

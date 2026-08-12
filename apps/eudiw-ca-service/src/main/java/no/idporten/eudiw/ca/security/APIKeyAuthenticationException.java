package no.idporten.eudiw.ca.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class APIKeyAuthenticationException extends AuthenticationException {

    private final String error;
    private HttpStatus httpStatus;


    APIKeyAuthenticationException(String msg) {
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

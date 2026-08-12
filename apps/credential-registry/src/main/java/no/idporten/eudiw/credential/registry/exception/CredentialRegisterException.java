package no.idporten.eudiw.credential.registry.exception;

import org.springframework.http.HttpStatus;

public class CredentialRegisterException extends RuntimeException{
    public static final String ERROR = "Error occured";

    private final String error;
    private HttpStatus httpStatus;

    public CredentialRegisterException(String error, String errorDescription, HttpStatus httpStatus) {
        this(error, errorDescription, httpStatus, null);
    }

    public CredentialRegisterException(String error, String errorDescription, HttpStatus httpStatus, Throwable cause) {
        super(errorDescription, cause);
        this.error = error;
        this.httpStatus = httpStatus;
    }

    public String getErrorDescription() {
        return super.getMessage();
    }

    public  HttpStatus getHttpStatus() {
        return httpStatus;
    }
    public String getError() {
        return error;
    }
}

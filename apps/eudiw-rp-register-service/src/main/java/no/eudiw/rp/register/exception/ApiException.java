package no.eudiw.rp.register.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RegisterServiceException {

    private final String error;
    private final String errorDescription;
    private final HttpStatus httpStatus;

    public ApiException(String error, String errorDescription, HttpStatus httpStatus) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.httpStatus = httpStatus;
    }
}

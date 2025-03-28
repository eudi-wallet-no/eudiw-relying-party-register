package no.idporten.eudiw.rp.ca.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CertificateAuthorityException extends RuntimeException {

    private String error;
    private String errorDescription;
    private HttpStatus httpStatus;


    public CertificateAuthorityException(String error, String errorDescription, HttpStatus httpStatus, Exception e) {
        super(e);
        this.error = error;
        this.errorDescription = errorDescription;
        this.httpStatus = httpStatus;
    }

    public CertificateAuthorityException(String error, String errorDescription, HttpStatus httpStatus) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.httpStatus = httpStatus;
    }

}

package no.idporten.eudiw.trustlist.service;


/**
 * Custom exception class for signing errors in the application. For XML/Document signing failures.
 */
public class SigningException extends RuntimeException {

    public SigningException(String message) {
        super(message);
    }

    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }

}

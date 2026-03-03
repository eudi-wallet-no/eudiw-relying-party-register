package no.idporten.eudiw.trustlist.exception;

/**
 * Custom exception class for application-specific errors (business logic).
 */
public class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }


}

package no.idporten.eudiw.rp.admin.service.exception;

public class AlreadyExistsException extends ErrorResponseException {
    public AlreadyExistsException(String errorDescription) {
        super("already_exists", errorDescription);
    }
}

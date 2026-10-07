package no.eudiw.rp.register.exception;

public class AlreadyExistsException extends RegisterServiceException {
    public AlreadyExistsException(String msg) {
        super(msg);
    }
}

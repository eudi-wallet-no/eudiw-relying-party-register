package no.eudiw.rp.register.exception;

public class NotFoundException extends RegisterServiceException {
    public NotFoundException(String msg) {
        super(msg);
    }
    public NotFoundException(String msg, String logMessage) {
        super(msg, logMessage);
    }
}

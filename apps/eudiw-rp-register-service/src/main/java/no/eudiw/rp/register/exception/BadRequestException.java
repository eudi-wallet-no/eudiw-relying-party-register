package no.eudiw.rp.register.exception;

public class BadRequestException extends RegisterServiceException {
    public BadRequestException(String msg) {
        super(msg);
    }
}

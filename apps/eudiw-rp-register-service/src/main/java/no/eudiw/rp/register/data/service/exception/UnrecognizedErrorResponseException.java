package no.eudiw.rp.register.data.service.exception;

public class UnrecognizedErrorResponseException extends ErrorResponseException {
    public UnrecognizedErrorResponseException(String msg) {
        super(msg);
    }
    public UnrecognizedErrorResponseException(String msg, Throwable e) {
        super(msg, e);
    }
}

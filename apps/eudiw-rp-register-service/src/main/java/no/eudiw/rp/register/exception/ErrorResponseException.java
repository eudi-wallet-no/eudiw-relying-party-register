package no.eudiw.rp.register.exception;

public class ErrorResponseException extends RegisterServiceException {
    public ErrorResponseException(String msg) {
        super(msg);
    }
    public ErrorResponseException(String msg, Throwable e) {
        super(msg, e);
    }
}

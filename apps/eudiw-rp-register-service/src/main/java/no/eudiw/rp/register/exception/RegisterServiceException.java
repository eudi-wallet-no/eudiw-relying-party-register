package no.eudiw.rp.register.exception;

public class RegisterServiceException extends RuntimeException {
    public RegisterServiceException() {
    }
    public RegisterServiceException(String msg) {
        super(msg);
    }
    public RegisterServiceException(String msg, Throwable e) {
        super(msg, e);
    }
}

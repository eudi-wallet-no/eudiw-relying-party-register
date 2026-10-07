package no.eudiw.rp.register.exception;

import lombok.Getter;

@Getter
public class RegisterServiceException extends RuntimeException {

    private String logMessage;

    public RegisterServiceException() {
    }

    public RegisterServiceException(String msg) {
        super(msg);
    }

    public RegisterServiceException(String msg, Throwable e) {
        super(msg, e);
    }

    public RegisterServiceException(String msg, String logMessage, Throwable e) {
        super(msg, e);
        this.logMessage = logMessage;
    }
    public RegisterServiceException(String msg, String logMessage) {
        super(msg);
        this.logMessage = logMessage;
    }

}

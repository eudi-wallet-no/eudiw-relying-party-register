package no.eudiw.rp.register.data.service.exception;

public class ServiceException extends RuntimeException {
    public ServiceException() {
    }
    public ServiceException(String msg) {
        super(msg);
    }
    public ServiceException(String msg, Throwable e) {
        super(msg, e);
    }
}

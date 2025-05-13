package no.idporten.eudiw.rp.admin.service.exception;

public class AdminServiceException extends RuntimeException {
    public AdminServiceException(String msg) {
        super(msg);
    }
    public AdminServiceException(String msg, Throwable e) {
        super(msg, e);
    }
}

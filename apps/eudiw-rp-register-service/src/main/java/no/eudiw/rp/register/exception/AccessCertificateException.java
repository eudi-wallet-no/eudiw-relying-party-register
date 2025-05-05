package no.eudiw.rp.register.exception;

public class AccessCertificateException extends RegisterServiceException {
    public AccessCertificateException(String msg) {
        super(msg);
    }
    public AccessCertificateException(String msg, Throwable e) {
        super(msg, e);
    }
}

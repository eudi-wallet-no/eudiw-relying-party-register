package no.eudiw.rp.register.exception;

public class CertificateConversionException extends RegisterServiceException {
    public CertificateConversionException(String msg) {
        super(msg);
    }
    public CertificateConversionException(String msg, Throwable e) {
        super(msg, e);
    }
}

package no.eudiw.rp.register.exception;

import no.eudiw.rp.register.data.service.exception.ServiceException;

public class CertificateConversionException extends ServiceException {
    public CertificateConversionException(String msg) {
        super(msg);
    }
    public CertificateConversionException(String msg, Throwable e) {
        super(msg, e);
    }
}

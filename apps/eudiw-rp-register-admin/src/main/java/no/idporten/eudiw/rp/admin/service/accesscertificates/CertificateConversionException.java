package no.idporten.eudiw.rp.admin.service.accesscertificates;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;

public class CertificateConversionException extends AdminServiceException {
    public CertificateConversionException(String msg) {
        super(msg);
    }
    public CertificateConversionException(String msg, Throwable e) {
        super(msg, e);
    }
}

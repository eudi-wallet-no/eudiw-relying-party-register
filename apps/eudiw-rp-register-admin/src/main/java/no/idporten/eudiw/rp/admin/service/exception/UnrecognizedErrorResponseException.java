package no.idporten.eudiw.rp.admin.service.exception;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;

public class UnrecognizedErrorResponseException extends AdminServiceException {
    public UnrecognizedErrorResponseException(String msg) {
        super(msg);
    }
    public UnrecognizedErrorResponseException(String msg, Throwable e) {
        super(msg, e);
    }
}

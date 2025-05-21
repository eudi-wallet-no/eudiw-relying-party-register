package no.idporten.eudiw.rp.admin.service.exception;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;

public class ErrorResponseException extends AdminServiceException {
    public ErrorResponseException(String msg) {
        super(msg);
    }
    public ErrorResponseException(String msg, Throwable e) {
        super(msg, e);
    }
}

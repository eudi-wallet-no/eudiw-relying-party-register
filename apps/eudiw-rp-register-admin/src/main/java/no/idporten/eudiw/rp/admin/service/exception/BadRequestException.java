package no.idporten.eudiw.rp.admin.service.exception;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;

public class BadRequestException extends AdminServiceException {
    public BadRequestException(String msg) {
        super(msg);
    }
    public BadRequestException(String msg, Throwable e) {
        super(msg, e);
    }
}

package no.idporten.eudiw.rp.admin.service.exception;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;

public class NotFoundException extends AdminServiceException {
    public NotFoundException(String msg) {
        super(msg);
    }
    public NotFoundException(String msg, Throwable e) {
        super(msg, e);
    }
}

package no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;

public class InvalidAuthorizationDetailsException extends AdminServiceException {
    public InvalidAuthorizationDetailsException(String msg) {
        super(msg);
    }
    public InvalidAuthorizationDetailsException(String msg, Throwable e) {
        super(msg, e);
    }
}

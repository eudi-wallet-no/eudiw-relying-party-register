package no.idporten.eudiw.rp.admin.service.exception;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;

public class UnauthorizedRequestException extends AdminServiceException {
    public UnauthorizedRequestException() {
        super("Unauthorized request (bad/missing API key?)");
    }
}

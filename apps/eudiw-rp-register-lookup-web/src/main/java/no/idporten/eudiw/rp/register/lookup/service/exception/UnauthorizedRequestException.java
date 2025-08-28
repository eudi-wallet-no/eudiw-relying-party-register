package no.idporten.eudiw.rp.register.lookup.service.exception;

import no.idporten.eudiw.rp.register.lookup.exception.LookupServiceException;

public class UnauthorizedRequestException extends LookupServiceException {
    public UnauthorizedRequestException() {
        super("Unauthorized request (bad/missing API key?)");
    }
}

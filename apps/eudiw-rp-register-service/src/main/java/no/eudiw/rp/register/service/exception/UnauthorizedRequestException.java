package no.eudiw.rp.register.service.exception;

import no.eudiw.rp.register.exception.RegisterServiceException;

public class UnauthorizedRequestException extends RegisterServiceException {
    public UnauthorizedRequestException() {
        super("Unauthorized request (bad/missing API key?)");
    }
}

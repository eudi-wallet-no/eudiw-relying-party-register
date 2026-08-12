package no.eudiw.rp.register.service.exception;

import no.eudiw.rp.register.exception.RegisterServiceException;

public class AlreadyExistsException extends RegisterServiceException {
    public AlreadyExistsException(String msg) {
        super(msg);
    }
}

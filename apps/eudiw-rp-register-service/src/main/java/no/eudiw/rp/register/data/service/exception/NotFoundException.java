package no.eudiw.rp.register.data.service.exception;

import no.eudiw.rp.register.exception.RegisterServiceException;

public class NotFoundException extends RegisterServiceException {
    public NotFoundException(String msg) {
        super(msg);
    }
}

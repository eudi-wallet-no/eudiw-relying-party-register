package no.eudiw.rp.register.service.exception;

import no.eudiw.rp.register.exception.RegisterServiceException;

public class BadRequestException extends RegisterServiceException {
    public BadRequestException(String msg) {
        super(msg);
    }
}

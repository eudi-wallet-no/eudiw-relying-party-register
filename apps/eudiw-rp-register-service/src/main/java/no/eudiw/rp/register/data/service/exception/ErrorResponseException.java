package no.eudiw.rp.register.data.service.exception;

import no.eudiw.rp.register.exception.RegisterServiceException;

public class ErrorResponseException extends RegisterServiceException {
    public ErrorResponseException(String msg) {
        super(msg);
    }
}

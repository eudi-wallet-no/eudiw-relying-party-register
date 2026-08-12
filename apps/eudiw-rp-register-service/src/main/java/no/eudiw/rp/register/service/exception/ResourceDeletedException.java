package no.eudiw.rp.register.service.exception;

import no.eudiw.rp.register.exception.RegisterServiceException;

public class ResourceDeletedException extends RegisterServiceException {
    public ResourceDeletedException(String msg) {
        super(msg);
    }
}

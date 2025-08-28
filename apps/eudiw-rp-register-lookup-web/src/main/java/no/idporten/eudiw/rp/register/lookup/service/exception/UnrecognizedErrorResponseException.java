package no.idporten.eudiw.rp.register.lookup.service.exception;


import no.idporten.eudiw.rp.register.lookup.exception.LookupServiceException;

public class UnrecognizedErrorResponseException extends LookupServiceException {
    public UnrecognizedErrorResponseException(String msg) {
        super(msg);
    }
    public UnrecognizedErrorResponseException(String msg, Throwable e) {
        super(msg, e);
    }
}

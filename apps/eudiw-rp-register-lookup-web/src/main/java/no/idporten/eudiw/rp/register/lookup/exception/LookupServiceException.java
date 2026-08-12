package no.idporten.eudiw.rp.register.lookup.exception;

public class LookupServiceException extends RuntimeException {
    public LookupServiceException(String msg) {
        super(msg);
    }
    public LookupServiceException(String msg, Throwable e) {
        super(msg, e);
    }
}

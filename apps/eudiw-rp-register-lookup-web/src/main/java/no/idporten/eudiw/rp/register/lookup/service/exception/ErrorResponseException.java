package no.idporten.eudiw.rp.register.lookup.service.exception;

public class ErrorResponseException extends LookupServiceException {
    public ErrorResponseException(String msg) {
        super(msg);
    }
    public ErrorResponseException(String msg, Throwable e) {
        super(msg, e);
    }
}

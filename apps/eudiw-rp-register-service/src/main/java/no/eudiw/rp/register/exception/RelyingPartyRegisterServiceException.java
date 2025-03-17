package no.eudiw.rp.register.exception;

public class RelyingPartyRegisterServiceException extends RuntimeException {

    public RelyingPartyRegisterServiceException(String msg) {
        super(msg);
    }

    public RelyingPartyRegisterServiceException(String msg, Throwable e) {
        super(msg, e);
    }
}

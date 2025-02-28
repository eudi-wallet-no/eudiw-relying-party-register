package no.eudiw.rp.register;

public class RelyingPartyRegisterServiceException extends RuntimeException {

    public RelyingPartyRegisterServiceException(String msg) {
        super(msg);
    }

    public RelyingPartyRegisterServiceException(String msg, Throwable e) {
        super(msg, e);
    }
}

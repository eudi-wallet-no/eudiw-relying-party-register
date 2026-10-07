package no.eudiw.rp.register.exception;

public class UnauthorizedRequestException extends RegisterServiceException {
    public UnauthorizedRequestException() {
        super("Unauthorized request (bad/missing API key?)");
    }
}

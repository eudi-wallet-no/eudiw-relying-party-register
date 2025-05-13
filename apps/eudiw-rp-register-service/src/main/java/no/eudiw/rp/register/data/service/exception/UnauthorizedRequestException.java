package no.eudiw.rp.register.data.service.exception;

public class UnauthorizedRequestException extends ServiceException {
    public UnauthorizedRequestException() {
        super("Unauthorized request (bad/missing API key?)");
    }
}

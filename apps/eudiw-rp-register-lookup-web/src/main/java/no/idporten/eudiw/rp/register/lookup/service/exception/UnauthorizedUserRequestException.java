package no.idporten.eudiw.rp.register.lookup.service.exception;

public class UnauthorizedUserRequestException extends LookupServiceException {
    public UnauthorizedUserRequestException() {
        super("Unauthorized request (missing API-key?)");
    }
}

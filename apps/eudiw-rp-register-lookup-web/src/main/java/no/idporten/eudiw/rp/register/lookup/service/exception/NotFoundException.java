package no.idporten.eudiw.rp.register.lookup.service.exception;

public class NotFoundException extends ErrorResponseException {
    public NotFoundException(String errorDescription) {
        super("not_found", errorDescription);
    }
}

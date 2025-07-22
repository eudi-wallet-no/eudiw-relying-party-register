package no.idporten.eudiw.rp.admin.service.exception;

public class NotFoundException extends ErrorResponseException {
    public NotFoundException(String errorDescription) {
        super("not_found", errorDescription);
    }
}

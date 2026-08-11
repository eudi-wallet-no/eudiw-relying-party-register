package no.idporten.eudiw.rp.admin.service.exception;

public class BadRequestException extends ErrorResponseException {
    public BadRequestException(String errorDescription) {
        super("invalid_request", errorDescription);
    }
}

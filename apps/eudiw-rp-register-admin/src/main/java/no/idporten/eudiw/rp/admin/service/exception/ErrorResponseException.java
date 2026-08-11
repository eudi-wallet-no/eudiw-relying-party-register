package no.idporten.eudiw.rp.admin.service.exception;

import lombok.Getter;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.web.resource.ErrorResponseResource;

@Getter
public class ErrorResponseException extends AdminServiceException {

    private final String error;
    private final String errorDescription;

    public ErrorResponseException(String error, String errorDescription) {
        super(errorDescription);
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public static ErrorResponseException fromResource(ErrorResponseResource errorResponseResource) {
        String error = errorResponseResource.error();
        String errorDesc = errorResponseResource.errorDescription();
        return switch (error) {
            case "not_found", "resource_deleted" -> new NotFoundException(errorDesc);
            case "already_exists" -> new AlreadyExistsException(errorDesc);
            case "bad_request", "invalid_request" -> new BadRequestException(errorDesc);
            default -> new ErrorResponseException(error, errorDesc);
        };
    }
}

package no.eudiw.rp.register.exception;

import lombok.extern.slf4j.Slf4j;
import no.eudiw.rp.register.data.service.exception.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@Slf4j
@ControllerAdvice
@Order(80)
public class ServiceExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRelyingPartyNotFoundException(
        NotFoundException e) {
        return AppExceptionHandler.errorResponseEntity(HttpStatus.NOT_FOUND, "not_found", e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
        BadRequestException e) {
        return AppExceptionHandler.errorResponseEntity(HttpStatus.BAD_REQUEST, "invalid_request", e.getMessage());
    }

    @ExceptionHandler(ResourceDeletedException.class)
    public ResponseEntity<ErrorResponse> handleResourceDeletedException(
        ResourceDeletedException e) {
        return AppExceptionHandler.errorResponseEntity(HttpStatus.GONE, "resource_deleted", e.getMessage());
    }

    @ExceptionHandler(CertificateConversionException.class)
    public ResponseEntity<ErrorResponse> handleCertificateConversionException(
        CertificateConversionException e) {
        return AppExceptionHandler.errorResponseEntity(HttpStatus.BAD_REQUEST, "invalid_request", e.getMessage());
    }


    private ResponseEntity<ErrorResponse> genericInternalErrorResponse(
        String errorDescription, Exception e) {
        log.error("{}: {}", errorDescription, e.getMessage(), e);
        return AppExceptionHandler.errorResponseEntity(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "server_error",
            errorDescription
        );
    }

    @ExceptionHandler(UnauthorizedRequestException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedRequestException(
        UnauthorizedRequestException e) {
        return genericInternalErrorResponse("Unauthorized request (missing/bad API key?)", e);
    }

    @ExceptionHandler(UnrecognizedErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleUnrecognizedErrorResponseException(
        UnrecognizedErrorResponseException e) {
        return genericInternalErrorResponse("Unrecognized error response from CA service", e);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleErrorResponseException(
        ErrorResponseException e) {
        return genericInternalErrorResponse("Request rejected by CA service", e);
    }


    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException e) {
        return genericInternalErrorResponse(e.getMessage(), e);
    }

}

package no.eudiw.rp.register.exception;

import lombok.extern.slf4j.Slf4j;
import no.eudiw.rp.register.api.resource.ErrorResponseResource;
import no.eudiw.rp.register.security.ApiKeyAuthenticationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
@Order(100)
public class AppExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseResource> handleException(Exception e) {
        log.error("Failed to process request", e);
        return errorResponseEntity(
            HttpStatus.INTERNAL_SERVER_ERROR, "server_error", "Unrecognized internal server error");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseResource> handleHttpMediaTypeNotSupportedException(
        HttpMediaTypeNotSupportedException e) {
        return errorResponseEntity(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "invalid_request", "HTTP media type not supported");
    }

    @ExceptionHandler(ApiKeyAuthenticationException.class)
    public ResponseEntity<ErrorResponseResource> handleApiException(ApiKeyAuthenticationException e) {
        log.warn("Unauthorized request: {}", e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
            .body(new ErrorResponseResource(e.getError(), e.getErrorDescription()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseResource> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException e) {
        return errorResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "invalid_request", "Unsupported HTTP method");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseResource> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, "invalid_request", "Requested resource not found");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseResource> handleNoResourceFoundException(NoResourceFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, "invalid_request", "Requested resource not found");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseResource> handleNoSuchElementException(NoSuchElementException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, "invalid_request", "Requested resource not found");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseResource> handleResponseStatusException(ResponseStatusException e) {
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), e.getReason());
    }

    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return "invalid_request";
        }
        return "server_error";
    }

    protected static ResponseEntity<ErrorResponseResource> errorResponseEntity(
        HttpStatusCode httpStatus, String error, String errorDescription) {
        return errorResponseEntity(httpStatus, new ErrorResponseResource(error, errorDescription));
    }

    protected static ResponseEntity<ErrorResponseResource> errorResponseEntity(
        HttpStatusCode httpStatus, ErrorResponseResource errorResponseResource) {
        return ResponseEntity.status(httpStatus).contentType(MediaType.APPLICATION_JSON).body(errorResponseResource);
    }
}

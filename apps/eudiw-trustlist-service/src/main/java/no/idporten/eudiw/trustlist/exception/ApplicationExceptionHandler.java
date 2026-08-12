package no.idporten.eudiw.trustlist.exception;

import no.idporten.eudiw.trustlist.service.SigningException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class ApplicationExceptionHandler {

    Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

    public static final String INVALID_REQUEST = "invalid_request";
    public static final String SERVER_ERROR = "server_error";

    // Our custom exception
    @ExceptionHandler(SigningException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleSigningException(SigningException e) {
        log.error("SigningException occurred: {}", e.getMessage(), e);
        return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server failed to process request");
    }

    // ApplicationException exception
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleApplicationException(ApplicationException e) {
        log.error("ApplicationException occurred: {}", e.getMessage(), e);
        return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server failed to process request");
    }


    // JsonSignException exception
    @ExceptionHandler(JsonSignException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleJsonSignException(JsonSignException e) {
        log.error("JsonSignException occurred: {}", e.getMessage(), e);
        return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server failed to sign trustlist");
    }

    // Spring 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return errorResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, INVALID_REQUEST, "Unsupported HTTP method");
    }

    // Spring 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring-exception som gir HTTP-feil
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), e.getReason());
    }

    // TODO: temp exception handler.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException occurred", e);
        return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, errorMessageForHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR), e.getMessage());
    }

    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        return SERVER_ERROR;
    }

    // Catch-all for unknown other RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleRuntime(RuntimeException e) {
        log.error("Unknown RuntimeException occurred: {}", e.getMessage(), e);
        return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server failed to process request");
    }

    protected static ResponseEntity<@NonNull ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, String error, String errorDescription) {
        return errorResponseEntity(httpStatus, new ErrorResponse(error, errorDescription));
    }

    protected static ResponseEntity<@NonNull ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, ErrorResponse errorResponse) {
        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}

package no.eudiw.rp.register.exception;

import lombok.extern.slf4j.Slf4j;
import no.eudiw.rp.register.security.APIKeyAuthenticationException;
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


/**
 * Top level exception handling for application.
 */
@Slf4j
@ControllerAdvice
@Order(100)
public class AppExceptionHandler {

    // last resort
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Failed to process request", e);
        return errorResponseEntity(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "server_error",
            "Unrecognized internal server error");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
        HttpMediaTypeNotSupportedException e) {
        String errorDescription = "HTTP media type not supported: " + e.getContentType();
        return errorResponseEntity(HttpStatus.BAD_REQUEST, "invalid_request", errorDescription);
    }

    // api key
    @ExceptionHandler(APIKeyAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleApiException(APIKeyAuthenticationException e) {
        log.warn("Unauthorized request: {}", e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(new ErrorResponse(e.getError(), e.getErrorDescription()));
    }

    // Spring 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return errorResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "invalid_request", "Unsupported HTTP method");
    }

    // Spring 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, "invalid_request", "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, "invalid_request", "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, "invalid_request", "Requested resource not found");
    }

    // Spring-exception som gir HTTP-feil
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), e.getReason());
    }


    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return "invalid_request";
        }
        return "server_error";
    }

    protected static ResponseEntity<ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, String error, String errorDescription) {
        return errorResponseEntity(httpStatus, new ErrorResponse(error, errorDescription));
    }

    protected static ResponseEntity<ErrorResponse> errorResponseEntity(HttpStatusCode httpStatus, ErrorResponse errorResponse) {
        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}

package no.idporten.eudiw.ca.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.ca.security.APIKeyAuthenticationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Top level exception handling for application.
 */
@Slf4j
@ControllerAdvice
@Order(100)
public class AppExceptionHandler {

    public static final String INVALID_REQUEST = "invalid_request";
    public static final String SERVER_ERROR = "server_error";

    // last resort
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Failed to process request", e);
            return errorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, "Server failed to process request");
    }

    @ExceptionHandler(CertificateAuthorityException.class)
    public ResponseEntity<ErrorResponse> handleCertificateAuthorityException(CertificateAuthorityException e) {
        return errorResponseEntity(e.getHttpStatus(), e.getError(), e.getErrorDescription());
    }

    // api key
    @ExceptionHandler(APIKeyAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleApiException(APIKeyAuthenticationException e) {
        log.warn("Unauthorized request: {}", e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(new ErrorResponse(e.getError(), e.getErrorDescription()));
    }

    // validation
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        return errorResponseEntity(HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList().getFirst());
    }

    // Spring 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return errorResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, INVALID_REQUEST, "Unsupported HTTP method");
    }
    // Spring 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return errorResponseEntity(HttpStatus.BAD_REQUEST, INVALID_REQUEST, "Malformed request");
    }

    // Spring 415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        return errorResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, INVALID_REQUEST, "Malformed request");
    }

    // Spring 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        return errorResponseEntity(HttpStatus.NOT_FOUND, INVALID_REQUEST, "Requested resource not found");
    }

    // Spring-exception som gir HTTP-feil
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), e.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {

        String bindingErrorMessages =
                e.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(","));
        return errorResponseEntity(e.getStatusCode(), errorMessageForHttpStatus(e.getStatusCode()), bindingErrorMessages);
    }

    protected String errorMessageForHttpStatus(HttpStatusCode httpStatus) {
        if (httpStatus.is4xxClientError()) {
            return INVALID_REQUEST;
        }
        return SERVER_ERROR;
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

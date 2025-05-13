package no.eudiw.rp.register.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@Order(90)
public class ApiViolationExceptionHandler {
    private ApiViolationExceptionHandler() {}

    private ResponseEntity<ErrorResponse> apiViolationErrorResponse(
        String errorDescription, Exception e) {
        log.warn("API violation exception: {}", errorDescription, e);
        return AppExceptionHandler.errorResponseEntity(
            HttpStatus.BAD_REQUEST, "invalid_request", errorDescription);
    }

    // on jackson deserialization failure (e.g. unrecognized properties,
    // or missing required properties)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e) {
        return apiViolationErrorResponse(
                "Resource contains unexpected fields and/or is missing required fields", e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
        ConstraintViolationException e) {
        return apiViolationErrorResponse("Resource contains invalid field value(s)", e);
    }

    // on jakarta constraint violation (i.e. properties are recognized, but values
    // are invalid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException e) {

        String bindingErrorMessages =
            e.getBindingResult()
             .getFieldErrors()
             .stream()
             .map(FieldError::getDefaultMessage)
             .filter(Objects::nonNull)
             .sorted() // for testing purposes.
             .collect(Collectors.joining(","));

        String errorDescription =
            "Provided resource violates following constraints: " + bindingErrorMessages;

        return apiViolationErrorResponse(errorDescription, e);
    }

    // on API method type mismatch, i.e. when Spring fails to instantiate controller
    // method parameters from the given request URL (e.g. when given UUID is
    // not a valid UUID).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e) {
        return apiViolationErrorResponse("HTTP request parameter type error", e);
    }
}

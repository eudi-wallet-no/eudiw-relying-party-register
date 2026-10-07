package no.eudiw.rp.register.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import no.eudiw.rp.register.api.resource.ErrorResponseResource;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseResource> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return apiViolationErrorResponse(
            "Resource contains unexpected fields and/or is missing required fields", e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseResource> handleConstraintViolationException(ConstraintViolationException e) {
        return apiViolationErrorResponse("Resource contains invalid field value(s)", e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseResource> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String bindingErrorMessages = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.joining(","));

        String errorDescription = "Provided resource violates following constraints: " + bindingErrorMessages;
        return apiViolationErrorResponse(errorDescription, e);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseResource> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e) {
        return apiViolationErrorResponse("HTTP request parameter type error", e);
    }

    private ResponseEntity<ErrorResponseResource> apiViolationErrorResponse(String errorDescription, Exception e) {
        log.warn("API violation exception: {}", errorDescription, e);
        return AppExceptionHandler.errorResponseEntity(HttpStatus.BAD_REQUEST, "invalid_request", errorDescription);
    }
}

package no.idporten.eudiw.rp.register.lookup.web;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnauthorizedUserRequestException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnrecognizedErrorResponseException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class LookupControllerAdvice {

    private String genericInternalError(String errorMsg, Exception e) {
        log.error("{}: {}", errorMsg, e.getMessage());
        return "errors/error";
    }

    // register service gave unrecognized error response
    @ExceptionHandler(UnrecognizedErrorResponseException.class)
    private String handleUnrecognizedErrorResponseException(
        UnrecognizedErrorResponseException e) {
        return genericInternalError(
            "Unrecognized error response from register service", e);
    }

    // something went wrong with the connection
    @ExceptionHandler(RestClientException.class)
    private String handleRestClientException(RestClientException e) {
        return genericInternalError(
            "Error connecting to (or reading success response from) register service API", e);
    }

    // on missing API key. should never happen since
    // API-key is provided by the lookup application
    @ExceptionHandler(UnauthorizedUserRequestException.class)
    private String handleUnauthorizedRequestException(UnauthorizedUserRequestException e) {
        return genericInternalError("Unauthorized request", e);
    }

    // 400 response from register service. should never
    // happen since we validate requests before sending
    @ExceptionHandler(ErrorResponseException.class)
    private String handleErrorResponseException(ErrorResponseException e) {
        return genericInternalError("Request rejected by register service", e);
    }

    // unexpected errors
    @ExceptionHandler(Exception.class)
    private String handleException(Exception e) {
        return genericInternalError("Unexpected/unrecognized error", e);
    }

    // if user fiddles with the id in "/details/{id}" and gives an invalid id
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    private String handleMethodArgumentTypeMismatchException() {
        return "errors/id_not_found";
    }

    // 404
    @ExceptionHandler({NoResourceFoundException.class,
                       NoHandlerFoundException.class}) // is this one deprecated?
    private String handleNoResourceFoundException_NoHandlerFoundException() {
        return "errors/404";
    }
}

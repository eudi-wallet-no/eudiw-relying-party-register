package no.idporten.eudiw.rp.register.lookup.web;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnrecognizedErrorResponseException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;

@Slf4j
@ControllerAdvice
public class LookupControllerAdvice {

    @ExceptionHandler(UnrecognizedErrorResponseException.class)
    public String handleUnrecognizedErrorResponseException(
        UnrecognizedErrorResponseException e, Model model) {
        log.error("Unrecognized error from register service: {}", e.getMessage());
        model.addAttribute("error_msg", "Unrecognized error from register service");
        return "error";
    }

    @ExceptionHandler(ErrorResponseException.class)
    public String handleErrorResponseException(ErrorResponseException e, Model model) {
        log.info("Register service error response: {}", e.getMessage());
        model.addAttribute("error_msg", "Request rejected by register service API");
        return "bad_request";
    }

    @ExceptionHandler(RestClientException.class)
    public String handleRestClientException(RestClientException e, Model model) {
        log.error("Error connecting to (or reading success response from) register service API: {}",
            e.getMessage());
        model.addAttribute("error_msg", "Error communicating with register service API");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Unexpected exception: {}", e.getMessage());
        model.addAttribute("error_msg", "Unexpected error");
        return "error";
    }

}

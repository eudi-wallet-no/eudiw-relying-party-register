package no.idporten.eudiw.rp.register.lookup.web.controllers;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.register.lookup.exception.LookupServiceException;
import no.idporten.eudiw.rp.register.lookup.service.exception.NotFoundException;
import no.idporten.eudiw.rp.register.lookup.service.exception.RelyingPartyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

@ControllerAdvice
@Slf4j
public class LookupWebControllerAdvice {

    public static final String requestedIdOrOrgnoAttrId = "requestedIdOrOrgnoAttr";
    public static final String rejectedIdAttrId = "rejectedIdAttr";

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoResourceFoundException(
        @SuppressWarnings("unused") NoResourceFoundException _unused) {
        return new ModelAndView("error/404");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e) {
        log.info("Method argument type mismatch", e);

        if (UUID.class.equals(e.getRequiredType()) && e.getValue() != null) {
            return new ModelAndView("error/invalid_id", rejectedIdAttrId, e.getValue());
        }
        return new ModelAndView("error/5xx", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RelyingPartyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleRelyingPartyNotFoundException(RelyingPartyNotFoundException e) {
        return new ModelAndView("error/relying_party_not_found",
                                requestedIdOrOrgnoAttrId,
                                e.getRequested());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFoundException(NotFoundException e) {
        log.warn("Unexpected not-found-exception", e);
        return new ModelAndView("error/404");
    }


    @ExceptionHandler(LookupServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAdminServiceException(LookupServiceException e) {
        log.error("Unhandled service-level exception", e);
        return new ModelAndView("error/5xx");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleException(Exception e) {
        log.error("Unexpected application-level exception", e);
        return new ModelAndView("error/5xx");
    }
}

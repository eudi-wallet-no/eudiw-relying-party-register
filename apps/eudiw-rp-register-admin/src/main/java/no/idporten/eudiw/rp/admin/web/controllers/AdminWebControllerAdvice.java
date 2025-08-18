package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.RelyingPartyNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

@ControllerAdvice
@Slf4j
public class AdminWebControllerAdvice {

    public static final String requestedIdOrOrgnoAttrId = "requestedIdOrOrgnoAttr";
    public static final String rejectedIdAttrId = "rejectedIdAttr";

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFoundException(
        @SuppressWarnings("unused") NoResourceFoundException _unused) {
        return new ModelAndView("error/404");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e) {
        log.info("Method argument type mismatch", e);

        if (UUID.class.equals(e.getRequiredType()) && e.getValue() != null) {
            return new ModelAndView("error/invalid_id", rejectedIdAttrId, e.getValue());
        }
        return new ModelAndView("error/5xx");
    }

    @ExceptionHandler(RelyingPartyNotFoundException.class)
    public ModelAndView handleRelyingPartyNotFoundException(RelyingPartyNotFoundException e) {
        return new ModelAndView("error/relying_party_not_found",
                                requestedIdOrOrgnoAttrId,
                                e.getRequested());
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFoundException(NotFoundException e) {
        log.warn("Unexpected not-found-exception", e);
        return new ModelAndView("error/404");
    }


    @ExceptionHandler(AdminServiceException.class)
    public ModelAndView handleAdminServiceException(AdminServiceException e) {
        log.error("Unhandled service-level exception", e);
        return new ModelAndView("error/5xx");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e) {
        log.error("Unexpected application-level exception", e);
        return new ModelAndView("error/5xx");
    }
}

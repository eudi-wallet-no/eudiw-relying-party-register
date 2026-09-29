package no.idporten.eudiw.rp.admin.web.controllers;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.exception.AlreadyExistsException;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.RelyingPartyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.UUID;

@ControllerAdvice
@Slf4j
public class AdminWebControllerAdvice {

    public static final String requestedIdOrOrgnoAttrId = "requestedIdOrOrgnoAttr";
    public static final String rejectedIdAttrId = "rejectedIdAttr";
    public static final String certificateId = "certificateId";

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
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
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleRelyingPartyNotFoundException(RelyingPartyNotFoundException e) {
        return new ModelAndView("error/relying_party_not_found",
                                requestedIdOrOrgnoAttrId,
                                e.getRequested());
    }

    @ExceptionHandler(CertificateNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleCertificateNotFoundException(CertificateNotFoundException e) {
        return new ModelAndView("error/certificate_not_found",
                Map.of(
                    requestedIdOrOrgnoAttrId, e.getRpId(),
                    certificateId, e.getCertificateId()
                ));
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFoundException(NotFoundException e) {
        log.warn("Unexpected not-found-exception", e);
        return new ModelAndView("error/404");
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ModelAndView handleAlreadyExistsException(AlreadyExistsException e) {
        log.info("Register service reported a conflict: {}", e.getMessage());
        return new ModelAndView("error/conflict");
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBadRequestException(BadRequestException e) {
        log.info("Register service rejected the request: {}", e.getMessage());
        return new ModelAndView("error/bad_request");
    }

    @ExceptionHandler(AdminServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAdminServiceException(AdminServiceException e) {
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

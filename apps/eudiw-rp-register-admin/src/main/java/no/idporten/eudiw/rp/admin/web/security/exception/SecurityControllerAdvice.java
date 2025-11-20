package no.idporten.eudiw.rp.admin.web.security.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@Slf4j
@Order(-1)
public class SecurityControllerAdvice {

    @ExceptionHandler({InsufficientAuthorityException.class,
                       AccessDeniedException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleInsufficientAuthorityException(Exception e) {
        log.info("User requested resource to which they were not authorized", e);
        return new ModelAndView("error/404");
    }
}

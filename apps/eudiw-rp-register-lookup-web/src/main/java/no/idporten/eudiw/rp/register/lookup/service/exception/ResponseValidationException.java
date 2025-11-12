package no.idporten.eudiw.rp.register.lookup.service.exception;

import jakarta.validation.ConstraintViolation;
import lombok.Getter;
import no.idporten.eudiw.rp.register.lookup.exception.LookupServiceException;

import java.util.HashSet;
import java.util.Set;

@Getter
public class ResponseValidationException extends LookupServiceException {

    private final Set<ConstraintViolation<?>> violations;

    public ResponseValidationException(
        String errorDescription, Set<? extends ConstraintViolation<?>> violations) {
        super(errorDescription);
        this.violations = new HashSet<>(violations);
    }
}

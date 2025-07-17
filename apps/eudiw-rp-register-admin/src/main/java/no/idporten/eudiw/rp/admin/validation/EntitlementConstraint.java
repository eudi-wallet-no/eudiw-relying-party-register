package no.idporten.eudiw.rp.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = EntitlementStringValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EntitlementConstraint {
    String message() default "invalid_entitlement";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

package no.idporten.eudiw.rp.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = SaneStringValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SaneStringConstraint {
    String message() default "unsane_string";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int maxLength() default 255;
    boolean nullable() default true;
}

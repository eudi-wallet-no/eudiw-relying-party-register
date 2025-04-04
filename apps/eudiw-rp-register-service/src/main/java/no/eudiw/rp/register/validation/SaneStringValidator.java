package no.eudiw.rp.register.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SaneStringValidator
    implements ConstraintValidator<SaneStringConstraint, String> {

    private static final String ALLOWED_CHARS_REGEX = "[a-zA-ZæøåÆØÅ0-9.,\\-:'\"&/ ]*";
    private int maxLength = 0;

    @Override
    public boolean isValid(String str, ConstraintValidatorContext ctx) {
        return str == null
                   || str.length() <= this.maxLength
                       && str.matches(ALLOWED_CHARS_REGEX);
    }

    @Override
    public void initialize(SaneStringConstraint saneStringConstraint) {
        this.maxLength = saneStringConstraint.maxLength();
    }
}

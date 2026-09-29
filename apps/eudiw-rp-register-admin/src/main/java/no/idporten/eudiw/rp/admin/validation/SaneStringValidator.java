package no.idporten.eudiw.rp.admin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SaneStringValidator
    implements ConstraintValidator<SaneStringConstraint, String> {

    public static final String ALLOWED_SYMBOLS = ".,-:'&/";
    private static final String ALLOWED_CHARS_REGEX =
        "[a-zA-ZæøåÆØÅ0-9 " + ALLOWED_SYMBOLS + "]*";
    private int maxLength = 0;
    private boolean nullable = false;

    @Override
    public boolean isValid(String str, ConstraintValidatorContext ctx) {
        return nullable && str == null
               || str != null
                   && str.length() <= this.maxLength
                       && str.matches(ALLOWED_CHARS_REGEX);
    }

    @Override
    public void initialize(SaneStringConstraint saneStringConstraint) {
        this.maxLength = saneStringConstraint.maxLength();
        this.nullable = saneStringConstraint.nullable();
    }
}

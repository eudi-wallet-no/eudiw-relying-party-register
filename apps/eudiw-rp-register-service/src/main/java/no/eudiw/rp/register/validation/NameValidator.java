package no.eudiw.rp.register.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NameValidator
    implements ConstraintValidator<NameConstraint, String> {

    private static final int NAME_MAX_LENGTH = 180;

    private static final String ALLOWED_CHARS = "a-zA-ZæøåÆØÅ0-9.,\\-:'\"&/ ";
    private static final String VALID_NAME_REGEX =
        String.format("[%s]{1,%d}", ALLOWED_CHARS, NAME_MAX_LENGTH);

    private static boolean isValidName(String name) {
        return name == null || !name.isBlank() && name.matches(VALID_NAME_REGEX);
    }

    @Override
    public boolean isValid(String name, ConstraintValidatorContext ctx) {
        return NameValidator.isValidName(name);
    }
}

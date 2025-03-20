package no.eudiw.rp.register.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.util.HtmlUtils;

public class SaneStringValidator
    implements ConstraintValidator<SaneStringConstraint, String> {

    private static final int VARCHAR_FIELD_MAX_LENGTH = 255;

    private static boolean isSaneString(String str) {
        return str == null ||
                   str.length() <= VARCHAR_FIELD_MAX_LENGTH
                       && HtmlUtils.htmlEscape(str).equals(str);
    }

    @Override
    public boolean isValid(String str, ConstraintValidatorContext ctx) {
        return SaneStringValidator.isSaneString(str);
    }
}

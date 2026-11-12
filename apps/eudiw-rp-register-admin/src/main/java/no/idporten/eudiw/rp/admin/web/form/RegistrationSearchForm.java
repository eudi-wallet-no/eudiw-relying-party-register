package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;

@Getter
public class RegistrationSearchForm {

    @Size(max = 255, message = "Ugyldig søk. Maks 255 teikn.")
    @SaneStringConstraint(message =
        "Ugyldig søk. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS,
        nullable = false)
    private String searchTerm = "";

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm.strip();
    }
}

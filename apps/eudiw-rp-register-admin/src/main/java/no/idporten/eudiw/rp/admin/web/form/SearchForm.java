package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;

import java.util.ArrayList;
import java.util.List;

public record SearchForm(
    @Size(max = 255, message = "Ugyldig søk. Maks 255 teikn.")
    @SaneStringConstraint(message =
        "Ugyldig søk. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS,
                          nullable = false)
    String searchTerm,
    boolean includeInactive,
    @NotNull
    List<String> requiredEntitlements
) {

    public static SearchForm empty() {
        return new SearchForm("", false, new ArrayList<>());
    }

    public SearchForm(
        String searchTerm,
        boolean includeInactive,
        List<String> requiredEntitlements
    ) {
        this.searchTerm = searchTerm.strip();
        this.includeInactive = includeInactive;
        this.requiredEntitlements = requiredEntitlements != null ? requiredEntitlements : new ArrayList<>();
    }
}

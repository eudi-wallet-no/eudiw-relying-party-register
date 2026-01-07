package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class SearchForm {
    @Size(max = 255, message = "Ugyldig søk. Maks 255 teikn.")
    @SaneStringConstraint(message =
        "Ugyldig søk. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS,
                          nullable = false)
    private String searchTerm = "";

    private boolean includeInactive = false;

    @NotNull
    private List<String> requiredEntitlements = new ArrayList<>();

    public static SearchForm empty() {
        return new SearchForm();
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm.strip();
    }

    public SearchRelyingPartyResource toResource() {
        return new SearchRelyingPartyResource()
                   .withSearchTerm(searchTerm)
                   .withIncludeInactive(includeInactive)
                   .withRequiredEntitlements(requiredEntitlements);
    }
}

package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.Size;
import lombok.With;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;

@With
public record SearchForm(
    @Size(max = 255, message = "søketerm må max være 255 tegn")
    @SaneStringConstraint(message =
        "søketerm får bare inneholde norske bokstaver, tall, mellemrom, og symbolene "
            + SaneStringValidator.ALLOWED_SYMBOLS,
                          nullable = false)
    String searchTerm,
    boolean includeInactive
) {

    public static SearchForm empty() {
        return new SearchForm("", false);
    }

    public SearchForm(String searchTerm, boolean includeInactive) {
        this.searchTerm = searchTerm.strip();
        this.includeInactive = includeInactive;
    }

    public SearchRelyingPartyResource toResource() {
        return new SearchRelyingPartyResource(this.searchTerm, this.includeInactive);
    }
}

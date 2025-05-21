package no.idporten.eudiw.rp.admin.web.resource;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.With;

@With
public record SearchForm(
    @Size(max = 255, message = "søketerm må max være 255 tegn!")
    @Pattern(regexp = ALLOWED_SEARCH_TERM_REGEX,
             message = "søketerm får bare inneholde norske bokstaver, tall, mellemrom, og symbolene "
                       + ALLOWED_SYMBOLS)
    String searchTerm,
    boolean includeInactive
) {

    private static final String ALLOWED_SYMBOLS = ".,-:'\"&/";
    private static final String ALLOWED_SEARCH_TERM_REGEX =
        "[a-zA-ZæøåÆØÅ0-9 " + ALLOWED_SYMBOLS + "]*";

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

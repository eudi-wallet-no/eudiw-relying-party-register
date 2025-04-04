package no.idporten.eudiw.rp.register.lookup.web.resource;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchForm(
    @Pattern(regexp = "\\d{0,9}", message = "søke-org.nr. må være 0-9 sifre!")
    String orgno,

    @Size(max = 255, message = "søkenavn må høyest være 255 tegn!")
    @Pattern(regexp = ALLOWED_NAME_REGEX,
             message = "søkenavn får bare indeholde norske bokstaver, tal, mellemrum, og tegnene "
                       + ALLOWED_SYMBOLS)
    String name,
    SearchSector searchSector,
    boolean includeInactive
) {
    private static final String ALLOWED_SYMBOLS = ".,-:'\"&/";
    private static final String ALLOWED_NAME_REGEX = "[a-zA-ZæøåÆØÅ0-9 " + ALLOWED_SYMBOLS + "]*";

    public enum SearchSector {
        PUBLIC  ("offentlig"),
        PRIVATE ("privat"),
        ANY     ("begge");
        public final String displayName;

        SearchSector(String displayName) {
            this.displayName = displayName;
        }
        private Boolean toDataBoolean() {
            return this == ANY ? null : this == PUBLIC;
        }
    }

    public static SearchForm empty() {
        return new SearchForm(null, null, SearchSector.ANY, false);
    }
    public SearchRelyingPartyResource toResource() {
        return new SearchRelyingPartyResource(
            this.orgno,
            this.name,
            this.searchSector.toDataBoolean(),
            this.includeInactive);
    }
    public SearchSector[] getSearchSectorOptions() {
        return SearchSector.values();
    }
}

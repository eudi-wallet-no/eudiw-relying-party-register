package no.idporten.eudiw.rp.admin.web.search.resultsview;

import lombok.Getter;

@Getter
public enum Ordering {
    NAME_ASC ("Navn"),
    ORGNO_ASC ("Organisasjonsnummer"),
    CREATED_ASC ("Opprettet"),
    LAST_UPDATED_ASC ("Oppdatert");

    private final String displayName;
    Ordering(String displayName) {
        this.displayName = displayName;
    }
}

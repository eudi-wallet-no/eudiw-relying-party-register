package no.eudiw.rp.register.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Law {

    private String legislativeIdentifier;

    private String legalBasis;

    protected Law() { }

    public Law(String legislativeIdentifier, String legalBasis) {
        this.legislativeIdentifier = legislativeIdentifier;
        this.legalBasis = legalBasis;
    }
}

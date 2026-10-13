package no.eudiw.rp.register.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Law {

    @Column(name = "legislative_identifier", nullable = false)
    private String legislativeIdentifier;

    @Column(name = "legal_basis")
    private String legalBasis;

    protected Law() { }

    public Law(String legislativeIdentifier, String legalBasis) {
        this.legislativeIdentifier = legislativeIdentifier;
        this.legalBasis = legalBasis;
    }
}

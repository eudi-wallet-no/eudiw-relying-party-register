package no.eudiw.rp.register.domain;

public record Law(String legislativeIdentifier, String legalBasis) {

    public Law(String legislativeIdentifier, String legalBasis) {
        this.legislativeIdentifier = legislativeIdentifier;
        this.legalBasis = legalBasis;
    }
}

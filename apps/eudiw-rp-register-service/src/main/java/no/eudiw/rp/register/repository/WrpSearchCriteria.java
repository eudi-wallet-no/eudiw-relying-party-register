package no.eudiw.rp.register.repository;

public record WrpSearchCriteria(String identifier, String legalName) {
    public static final String IDENTIFIER = "identifier";
    public static final String LEGAL_NAME = "legalName";
}

package no.eudiw.rp.register.domain;

public record Identifier(String type, String identifier) {

    public Identifier(String type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }
}

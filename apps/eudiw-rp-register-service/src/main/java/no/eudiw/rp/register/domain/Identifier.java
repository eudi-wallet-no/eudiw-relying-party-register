package no.eudiw.rp.register.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Identifier {

    private String type;

    private String identifier;

    protected Identifier() { }

    public Identifier(String type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }
}

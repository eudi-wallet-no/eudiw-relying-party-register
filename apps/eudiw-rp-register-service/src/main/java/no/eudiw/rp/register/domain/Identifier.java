package no.eudiw.rp.register.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Identifier {

    @Column(name = "identifier_type", nullable = false)
    private String type;

    @Column(name = "identifier_value", nullable = false)
    private String identifier;

    protected Identifier() { }

    public Identifier(String type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }
}

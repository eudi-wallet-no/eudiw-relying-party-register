package no.eudiw.rp.register.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Policy {

    private String type;

    private String policyURI;

    protected Policy() { }

    public Policy(String type, String policyURI) {
        this.type = type;
        this.policyURI = policyURI;
    }
}

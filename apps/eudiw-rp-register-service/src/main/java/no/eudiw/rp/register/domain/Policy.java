package no.eudiw.rp.register.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Policy {

    @Column(name = "policy_type", nullable = false)
    private String type;

    @Column(name = "policy_uri", nullable = false)
    private String policyURI;

    protected Policy() { }

    public Policy(String type, String policyURI) {
        this.type = type;
        this.policyURI = policyURI;
    }
}

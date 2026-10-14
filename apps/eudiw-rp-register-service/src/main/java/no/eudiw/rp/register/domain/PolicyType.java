package no.eudiw.rp.register.domain;

import lombok.Getter;

@Getter
public enum PolicyType {
    TRUST_SERVICE_PRACTICE_STATEMENT("Trust Service Practice Statement"),
    TERMS_AND_CONDITIONS("Terms and Conditions"),
    PRIVACY_STATEMENT("Privacy statement"),
    PRIVACY_POLICY("Privacy policy"),
    COOKIE_POLICY("Cookie policy");

    private final String label;

    PolicyType(String label) {
        this.label = label;
    }

}

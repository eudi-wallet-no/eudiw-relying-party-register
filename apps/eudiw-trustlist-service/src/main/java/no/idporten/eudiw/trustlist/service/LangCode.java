package no.idporten.eudiw.trustlist.service;

public enum LangCode {
    NO("no"),
    EN("en");

    private final String code;

    LangCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

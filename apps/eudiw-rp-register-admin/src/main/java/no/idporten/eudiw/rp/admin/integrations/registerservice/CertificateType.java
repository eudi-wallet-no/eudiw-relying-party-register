package no.idporten.eudiw.rp.admin.integrations.registerservice;

public enum CertificateType {
    ACCESS("access"),
    ISSUER("issuer");

    private final String pathSegment;

    CertificateType(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String pathSegment() {
        return pathSegment;
    }
}

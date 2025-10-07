package no.idporten.eudiw.rp.admin.web.security.oidcusers;

public record SelfServiceReportee(
    String orgno,
    String name,
    boolean publicSector
) { }

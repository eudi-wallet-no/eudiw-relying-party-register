package no.idporten.eudiw.rp.admin.web.security.oidcusers;

import org.springframework.security.core.GrantedAuthority;

public record ReporteeAuthority(
    String orgno,
    String name,
    String credentialIssuerUrl,
    boolean publicSector
) implements GrantedAuthority {
    @Override
    public String getAuthority() {
        return "rw:orgno:%s".formatted(orgno);
    }
}

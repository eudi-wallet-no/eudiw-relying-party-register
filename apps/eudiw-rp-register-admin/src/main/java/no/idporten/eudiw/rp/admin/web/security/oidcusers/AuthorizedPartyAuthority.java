package no.idporten.eudiw.rp.admin.web.security.oidcusers;

import org.springframework.security.core.GrantedAuthority;

public record AuthorizedPartyAuthority(
    String orgno,
    String name,
    boolean publicSector
) implements GrantedAuthority {
    @Override
    public String getAuthority() {
        return "rw:orgno:%s".formatted(orgno);
    }
}

package no.idporten.eudiw.rp.admin.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eudiw-admin-web.entra")
public record EntraProperties(
    boolean enabled,
    String readAccess,
    String writeAccess
) {
}
package no.idporten.eudiw.rp.admin.web.security.entraid;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eudiw-admin-web.entra")
public record EntraIdProperties(
    boolean enabled,
    String writeAccess
) {
}

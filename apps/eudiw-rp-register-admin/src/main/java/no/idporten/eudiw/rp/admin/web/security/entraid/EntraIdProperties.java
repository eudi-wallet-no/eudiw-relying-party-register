package no.idporten.eudiw.rp.admin.web.security.entraid;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eudiw-admin-web.security.entra")
public record EntraIdProperties(
    String writeAccess
) { }

package no.idporten.eudiw.credential.registry.configuration;


import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("credential-registry")
public record ConfigProperties(
        @DefaultValue("5s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout,
        @NotNull List<String> credentialIssuerServers

) {

}
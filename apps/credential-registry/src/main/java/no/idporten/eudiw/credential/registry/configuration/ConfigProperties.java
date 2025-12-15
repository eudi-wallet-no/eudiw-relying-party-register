package no.idporten.eudiw.credential.registry.configuration;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("credential-registry")
public record ConfigProperties(
        @DefaultValue("5s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout,
        @NotNull URI rpRegisterServiceUrl,
        @NotBlank String apiKeyHeaderId,
        @NotBlank String apiKeyValue


) {

}
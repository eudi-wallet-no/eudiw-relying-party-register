package no.idporten.eudiw.rp.register.lookup.service.credentialsservice.config;

import jakarta.validation.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-lookup-web.credentials-service")
public record CredentialsServiceProperties(
    @Valid
    CredentialsRegistryApi credentialsRegistryApi,
    @Valid
    RestClient restClient
) {
    public record CredentialsRegistryApi(
        @NotBlank String credentialsRegistryBaseUri
    ) { }

    public record RestClient(
        @Min(0) long connectTimeoutMillis,
        @Min(0) long readTimeoutMillis
    ) { }
}

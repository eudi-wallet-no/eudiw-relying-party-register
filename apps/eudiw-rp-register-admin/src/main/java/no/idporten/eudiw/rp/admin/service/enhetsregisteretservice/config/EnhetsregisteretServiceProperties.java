package no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "eudiw-admin-web.enhetsregisteret-service")
public record EnhetsregisteretServiceProperties(
    @NotNull
    URI enhetsregisteretApiBaseUri,
    @Valid
    RestClient restClient
) {
    public record RestClient(
        @Min(0) int connectTimeoutMillis,
        @Min(0) int readTimeoutMillis
    ) { }
}

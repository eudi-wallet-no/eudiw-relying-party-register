package no.eudiw.rp.register.data.service.enhetsregisteretservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@Validated

@ConfigurationProperties(prefix = "eudiw-rp-register-service.enhetsregisteret-service")
public record EnhetsregisteretServiceProperties(
    @NotNull
    List<@NotBlank String> knownPublicSectorCodes,
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

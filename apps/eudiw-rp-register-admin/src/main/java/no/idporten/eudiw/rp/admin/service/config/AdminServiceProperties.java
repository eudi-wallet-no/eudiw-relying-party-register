package no.idporten.eudiw.rp.admin.service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-admin-web.service")
public record AdminServiceProperties(
    RestClientDefaults restClientDefaults,
    String apiSearchEndpoint
) {
    @ConfigurationProperties(prefix = "eudiw-admin-web.service.rest-client")
    public record RestClientDefaults(
        @NotBlank String registerServiceApiBaseUri,
        @NotBlank String apiKeyHeaderId,
        @NotBlank String apiKeyValue,
        @Min(0) long connectTimeoutMillis,
        @Min(0) long readTimeoutMillis
    ) { }
}

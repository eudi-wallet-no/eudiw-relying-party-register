package no.idporten.eudiw.rp.register.lookup.service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-lookup-web.service")
public record LookupServiceProperties(
    RestClientDefaults restClientDefaults,
    String apiSearchEndpoint
) {
    @ConfigurationProperties(prefix = "eudiw-rp-register-lookup-web.service.rest-client-defaults")
    public record RestClientDefaults(
        @NotBlank String registerServiceApiBaseUri,
        @NotBlank String apiKeyHeaderId,
        @NotBlank String apiKeyValue,
        @Min(0) long connectTimeoutMillis,
        @Min(0) long readTimeoutMillis
    ) { }
}

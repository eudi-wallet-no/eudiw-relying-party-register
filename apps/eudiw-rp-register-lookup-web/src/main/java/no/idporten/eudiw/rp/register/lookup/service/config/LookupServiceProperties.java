package no.idporten.eudiw.rp.register.lookup.service.config;

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
        String registerServiceApiBaseUri,
        String apiKeyHeaderId,
        String apiKeyValue,
        long connectTimeoutMillis,
        long readTimeoutMillis
    ) { }
}

package no.idporten.eudiw.rp.register.lookup.service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-lookup-web.relying-parties-service")
public record RelyingPartiesServiceProperties(
    RegisterServiceApi registerServiceApi,
    RestClient restClient
) {
    public record RegisterServiceApi(
        @NotBlank String registerServiceBaseUri,
        @NotBlank String apiKeyHeaderId,
        @NotBlank String apiKeyValue
    ) { }

    public record RestClient(
        @Min(0) long connectTimeoutMillis,
        @Min(0) long readTimeoutMillis
    ) { }
}

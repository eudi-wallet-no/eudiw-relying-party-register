package no.eudiw.rp.register.data.service.accesscertificates;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-service.csr-service")
public record RelyingPartyCsrServiceProperties(
    @NotNull CaServiceApiProperties caServiceApi,
    @NotNull RestClientProperties restClient
) {
    @ConfigurationProperties(prefix = "eudiw-rp-register-service.csr-service.ca-service-api")
    public record CaServiceApiProperties(
        @NotBlank String caServiceBaseUri,
        @NotBlank String apiKeyHeaderId,
        @NotBlank String apiKeyValue
    ) { }

    @ConfigurationProperties(prefix = "eudiw-rp-register-service.csr-service.rest-client")
    public record RestClientProperties(
        @Min(0) long connectTimeoutMillis,
        @Min(0) long readTimeoutMillis
    ) { }
}

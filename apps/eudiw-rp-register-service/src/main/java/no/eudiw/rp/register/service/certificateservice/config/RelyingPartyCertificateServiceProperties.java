package no.eudiw.rp.register.service.certificateservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-service.csr-service")
public record RelyingPartyCertificateServiceProperties(
    @Valid CaServiceApi caServiceApi,
    @Valid RestClient restClient,
    @NotEmpty String accessCertificateCaId
) {
    public record CaServiceApi(
        @NotBlank String caServiceBaseUri,
        @NotBlank String apiKeyHeaderId,
        @NotBlank String apiKeyValue
    ) { }
    public record RestClient(
        @Min(0) long connectTimeoutMillis,
        @Min(0) long readTimeoutMillis
    ) { }
}

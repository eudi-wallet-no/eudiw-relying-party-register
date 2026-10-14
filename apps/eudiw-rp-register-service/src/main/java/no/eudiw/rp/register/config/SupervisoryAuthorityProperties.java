package no.eudiw.rp.register.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-service.supervisory-authority")
public record SupervisoryAuthorityProperties(
    @NotBlank String name,
    @NotBlank String country,
    String phone,
    String email,
    String formUri
) { }

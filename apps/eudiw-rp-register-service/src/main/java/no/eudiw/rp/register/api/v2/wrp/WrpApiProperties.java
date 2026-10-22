package no.eudiw.rp.register.api.v2.wrp;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-service.wrp")
public record WrpApiProperties(
    @NotBlank String issuer,
    @NotBlank String registryUri,
    @NotBlank String jwksUrl,
    @NotBlank String signingKeystore,
    @NotBlank String signingKeyId
) { }

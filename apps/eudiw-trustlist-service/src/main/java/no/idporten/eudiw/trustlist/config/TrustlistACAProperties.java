package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "trustlist-service.tsl-aca")
public record TrustlistACAProperties(@NotEmpty String path,
                                     @NotEmpty String keystore,
                                     @Valid @NotNull ListAndSchemeInformation schemeInformation,
                                     @Valid @NotNull Map<String, TrustedEntity> trustedEntities
) { }

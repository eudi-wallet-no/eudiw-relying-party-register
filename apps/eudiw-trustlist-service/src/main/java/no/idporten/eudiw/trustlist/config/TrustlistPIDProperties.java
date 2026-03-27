package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation;
import no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "trustlist-service.tsl-pid")
public record TrustlistPIDProperties(@NotEmpty String path,
                                     @NotEmpty String keystore,
                                     @Valid @NotNull ListAndSchemeInformation schemeInformation,
                                     @Valid @NotNull List<TrustedEntity> trustedEntities
) { }
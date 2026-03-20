package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "trustlist-service.tsl-aca")
public record TrustListACAProperties(@NotEmpty String path,
                                     @NotEmpty String keystore,
                                     @Valid @NotNull TLSchemeInformation schemeInformation,
                                     @Valid @NotNull List<TrustedEntity> trustedEntities
) { }

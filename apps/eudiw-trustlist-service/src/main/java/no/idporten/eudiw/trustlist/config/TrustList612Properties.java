package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import no.idporten.eudiw.trustlist.domain.TLServiceProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "trustlist-service.tsl612")
public record TrustList612Properties(@NotEmpty String trustlistPath,
                                     @NotEmpty String trustlistPathXtsl,
                                     @NotEmpty String trustlistPathSha2,
                                     @NotEmpty String keystore,
                                     @Valid @NotNull TLSchemeInformation schemeInformation,
                                     @Valid TLServiceProvider serviceProvider

) { }

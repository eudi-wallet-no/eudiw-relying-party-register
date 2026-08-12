package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.etsi612.TLSchemeInformation;
import no.idporten.eudiw.trustlist.domain.etsi612.TLServiceProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "trustlist-service.tsl612")
public record Trustlist612Properties(@NotEmpty String trustlistPath,
                                     @NotEmpty String trustlistPathXtsl,
                                     @NotEmpty String trustlistPathSha2,
                                     @NotEmpty String keystore,
                                     @Valid @NotNull TLSchemeInformation schemeInformation,
                                     @Valid Map<String, TLServiceProvider> serviceProviders

) { }

package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "trustlist-service.tsl-aca")
public record TrustListACAProperties(@NotEmpty String path,
                                     @Valid @NotNull TLSchemeInformation schemeInformation

) { }

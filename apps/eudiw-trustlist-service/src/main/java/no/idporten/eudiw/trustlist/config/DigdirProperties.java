package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.Address;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("trustlist-service.digdir")
public record DigdirProperties(@NotBlank String nameNo, @NotBlank String nameEn, @NotBlank String email, @NotBlank String web, @Valid @NotNull Address postalAddress) {
}

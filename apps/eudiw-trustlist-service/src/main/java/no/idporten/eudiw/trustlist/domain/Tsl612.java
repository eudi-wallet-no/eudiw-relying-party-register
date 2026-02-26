package no.idporten.eudiw.trustlist.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Tsl612(@NotBlank String trustlistPath,
                     @NotBlank String trustlistPathXtsl,
                     @NotBlank String trustlistPathSha2,
                     @Valid @NotNull TLSchemeInformation schemeInformation,
                     @Valid TLServiceProvider serviceProvider) {
}

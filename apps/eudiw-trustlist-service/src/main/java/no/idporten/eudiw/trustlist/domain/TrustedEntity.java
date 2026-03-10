package no.idporten.eudiw.trustlist.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public record TrustedEntity(@Valid @NotNull TrustedEntityInformation trustedEntityInformation) {
}

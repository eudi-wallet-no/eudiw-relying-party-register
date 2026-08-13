package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public record TrustedEntity(@Valid @NotNull TrustedEntityInformation trustedEntityInformation,
                            @Valid List<TrustedEntityService> trustedEntityServices) {
}

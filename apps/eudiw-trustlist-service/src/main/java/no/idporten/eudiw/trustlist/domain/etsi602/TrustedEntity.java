package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;


public record TrustedEntity(TrustedEntityInformation trustedEntityInformation,
                            @Valid @NotEmpty List<TrustedEntityService> trustedEntityServices) {
}

package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record Trustlist(@NotEmpty String path,
                        @NotEmpty String keystore,
                        @Valid @NotNull ListAndSchemeInformation schemeInformation,
                        @Valid @NotNull Map<String, TrustedEntity> trustedEntities) {
}

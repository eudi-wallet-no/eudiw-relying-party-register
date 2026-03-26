package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.constraints.NotBlank;

public record ServiceTypeIdentifier(
        @NotBlank String a
) {
}

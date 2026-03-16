package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.constraints.NotBlank;

public record InformationUri(
        @NotBlank String a,
        String b,
        @NotBlank String c
) {
}

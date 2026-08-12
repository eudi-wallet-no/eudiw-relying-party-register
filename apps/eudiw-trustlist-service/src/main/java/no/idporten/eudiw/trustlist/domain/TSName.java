package no.idporten.eudiw.trustlist.domain;

import jakarta.validation.constraints.NotBlank;

public record TSName(@NotBlank String langNo, @NotBlank String langEn) {
}

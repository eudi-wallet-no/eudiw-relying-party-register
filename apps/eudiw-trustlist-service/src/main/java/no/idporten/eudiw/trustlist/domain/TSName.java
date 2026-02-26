package no.idporten.eudiw.trustlist.domain;

import jakarta.validation.constraints.NotBlank;

public record TSName(String langNo, @NotBlank String langEn) {
}

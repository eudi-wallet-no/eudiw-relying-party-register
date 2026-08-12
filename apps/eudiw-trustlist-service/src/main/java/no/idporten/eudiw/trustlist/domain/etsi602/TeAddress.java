package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.constraints.NotBlank;

public record TeAddress(
        @NotBlank String phoneNumber
) {

}

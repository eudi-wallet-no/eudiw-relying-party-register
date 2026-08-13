package no.idporten.eudiw.trustlist.domain.etsi602;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrustedEntityInformation(
        @NotBlank String teName,
        @NotBlank String teTradeName,
        @Valid @NotNull InformationUri informationUri,
        TeAddress teAddress // this exists for pid phone number
        ) {
}

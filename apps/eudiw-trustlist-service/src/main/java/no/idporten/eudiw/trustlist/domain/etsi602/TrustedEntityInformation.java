package no.idporten.eudiw.trustlist.domain.etsi602;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TrustedEntityInformation(
        @NotEmpty String teName,
        @Valid @NotNull InformationUri informationUri
        ) {
}

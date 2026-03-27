package no.idporten.eudiw.trustlist.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TLServiceProvider(@Valid @NotNull TSName name, @Valid @NotNull TSName tradeName, @Valid @NotNull TSUri informationUri, @Valid List<TLService> services, @NotBlank String email, @NotBlank String website, @Valid @NotNull Address postalAddress) {

}

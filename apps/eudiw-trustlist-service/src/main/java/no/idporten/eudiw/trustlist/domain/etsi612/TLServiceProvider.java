package no.idporten.eudiw.trustlist.domain.etsi612;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.domain.Address;
import no.idporten.eudiw.trustlist.domain.TSName;
import no.idporten.eudiw.trustlist.domain.TSUri;

import java.util.List;

public record TLServiceProvider(@Valid @NotNull TSName name, @Valid @NotNull TSName tradeName, @Valid @NotNull TSUri informationUri, @Valid List<TLService> services, @NotBlank String email, @NotBlank String website, @Valid @NotNull Address postalAddress) {

}

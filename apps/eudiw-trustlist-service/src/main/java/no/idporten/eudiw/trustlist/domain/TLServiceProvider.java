package no.idporten.eudiw.trustlist.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TLServiceProvider(@Valid @NotNull TSName name, @Valid @NotNull TSName tradeName, @Valid TSUri informationUri, List<TLService> services) {

}

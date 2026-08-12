package no.idporten.eudiw.trustlist.domain.etsi602;

import jakarta.validation.constraints.NotNull;

import java.net.URI;

public record ServiceTypeIdentifier(
        @NotNull URI a
) {
}

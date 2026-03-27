package no.idporten.eudiw.trustlist.domain.etsi602;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ServiceInformation(
        ServiceTypeIdentifier serviceTypeIdentifier,
        @Valid @NotNull ServiceName serviceName,
        @Valid @NotNull ServiceDigitalIdentity serviceDigitalIdentity
        ) {
}

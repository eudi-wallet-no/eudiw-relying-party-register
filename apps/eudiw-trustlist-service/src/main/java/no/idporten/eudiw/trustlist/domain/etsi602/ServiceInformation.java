package no.idporten.eudiw.trustlist.domain.etsi602;


import jakarta.validation.Valid;

public record ServiceInformation(
        ServiceTypeIdentifier serviceTypeIdentifier,
        @Valid ServiceName serviceName,
        @Valid ServiceDigitalIdentity serviceDigitalIdentity
        ) {
}

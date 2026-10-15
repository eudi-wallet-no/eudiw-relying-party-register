package no.eudiw.rp.register.domain;

public record IntermediaryReference(
    String serviceIdentifier,
    String serviceTradeName,
    String registryURI
) { }

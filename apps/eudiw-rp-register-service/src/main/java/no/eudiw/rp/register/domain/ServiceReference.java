package no.eudiw.rp.register.domain;

import java.util.List;

public record ServiceReference(
    List<Identifier> identifier,
    String serviceIdentifier,
    String registryURI
) {
    public ServiceReference {
        identifier = List.copyOf(identifier);
    }
}

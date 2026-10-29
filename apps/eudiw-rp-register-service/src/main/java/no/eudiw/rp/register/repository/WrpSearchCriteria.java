package no.eudiw.rp.register.repository;

public record WrpSearchCriteria(
    String identifier,
    String legalName,
    String serviceIdentifier,
    String tradeName,
    String policy,
    String entitlement,
    String providedAttestation
) {
}

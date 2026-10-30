package no.eudiw.rp.register.api.v2.wrp;

public record WrpCollectionFilters(
    String identifier,
    String legalName,
    String serviceIdentifier,
    String tradeName,
    String policy,
    String intendedUseIdentifier,
    String entitlement,
    String providedAttestation,
    String usesIntermediary,
    Boolean isIntermediary
) {
}

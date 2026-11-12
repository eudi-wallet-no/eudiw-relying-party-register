package no.idporten.eudiw.rp.admin.web.resource;

import java.util.List;
import java.util.UUID;

public record RegistrationSearchResult(
    UUID id,
    String orgno,
    String orgName,
    String tradeName,
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,
    long createdMs,
    long lastUpdatedMs,
    int accessCertificateCount,
    int issuerCertificateCount
) { }

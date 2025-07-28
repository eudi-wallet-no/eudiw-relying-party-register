package no.idporten.eudiw.rp.admin.web.search.resultsview;

import java.math.BigInteger;
import java.util.UUID;

public record RelyingPartyAccessCertificateSummary(
    BigInteger serialNo,
    String subjectDn,
    String issuerDn,
    long validFromMs,
    long validUntilMs,
    UUID id
) { }

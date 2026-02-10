package no.idporten.eudiw.rp.admin.web.search.resultsview;

import java.math.BigInteger;
import java.util.UUID;

public record RelyingPartyCertificateSummary(
    String entitlement,
    BigInteger serialNo,
    String subjectDn,
    String issuerDn,
    long validFromMs,
    long validUntilMs,
    UUID id,
    int revocationStatus,
    StatusDisplayData status
) {
    public record StatusDisplayData(String text, String style) { }
}

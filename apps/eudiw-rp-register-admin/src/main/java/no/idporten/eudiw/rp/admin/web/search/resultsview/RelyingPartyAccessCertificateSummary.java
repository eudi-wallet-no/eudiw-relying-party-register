package no.idporten.eudiw.rp.admin.web.search.resultsview;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

public record RelyingPartyAccessCertificateSummary(
    X509Certificate certificate,
    BigInteger serialNo,
    String subjectDn,
    long validFromMs,
    long validUntilMs
) { }

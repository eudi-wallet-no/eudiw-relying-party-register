package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyIssuerCertificateSummary;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    String entitlement,

    @JsonProperty("certificates")
    List<RelyingPartyCertificateResource> certificates
) {

    public List<RelyingPartyIssuerCertificateSummary> toIssuerCertificateSummaries() {
        if (certificates == null || certificates.isEmpty()) {
            return List.of();
        }

        return certificates.stream()
            .map(certificate -> certificate.toIssuerSummary(entitlement))
            .toList();
    }
}

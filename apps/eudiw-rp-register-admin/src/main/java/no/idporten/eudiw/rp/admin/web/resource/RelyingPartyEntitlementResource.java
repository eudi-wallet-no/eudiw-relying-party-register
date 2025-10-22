package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyIssuerCertificateSummary;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    String entitlement,

    @JsonProperty("display_name")
    String displayName,

    @JsonProperty("certificates")
    List<RelyingPartyCertificateResource> certificates
) {
    public RelyingPartyEntitlementResource(String entitlement) {
        this(entitlement, entitlement, new ArrayList<>());
    }

    public List<RelyingPartyIssuerCertificateSummary> toIssuerCertificateSummaries() {
        return certificates == null ? List.of() :
            certificates.stream()
                        .map(certificate -> certificate.toIssuerSummary(entitlement))
                        .toList();
    }
}

package no.idporten.eudiw.rp.admin.web.resource.certificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyAccessCertificateSummary;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyAccessCertificatesResource(
    @Valid
    @NotNull(message = "null_certificates")
    @JsonProperty(value = "certificates", required = true)
    List<RelyingPartyCertificateResource> certificates
) {
    public List<RelyingPartyAccessCertificateSummary> toSummaries() {
        return this.certificates
                   .stream()
                   .map(RelyingPartyCertificateResource::toSummary)
                   .toList();
    }
}

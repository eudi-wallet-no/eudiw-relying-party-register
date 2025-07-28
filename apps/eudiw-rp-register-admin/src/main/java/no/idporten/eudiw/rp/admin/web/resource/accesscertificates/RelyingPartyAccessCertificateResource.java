package no.idporten.eudiw.rp.admin.web.resource.accesscertificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyAccessCertificateSummary;

import java.security.cert.X509Certificate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyAccessCertificateResource(
    @JsonProperty(value = "certificate", required = true)
    @JsonDeserialize(using = X509CertificateJsonDeserializer.class)
    X509Certificate certificate,
    @JsonProperty(value = "id", required = true)
    UUID id
) {
    public RelyingPartyAccessCertificateSummary toSummary() {
        return new RelyingPartyAccessCertificateSummary(
            this.certificate.getSerialNumber(),
            this.certificate.getSubjectX500Principal().getName(),
            this.certificate.getIssuerX500Principal().getName(),
            this.certificate.getNotBefore().toInstant().toEpochMilli(),
            this.certificate.getNotAfter().toInstant().toEpochMilli(),
            this.id
        );
    }
}

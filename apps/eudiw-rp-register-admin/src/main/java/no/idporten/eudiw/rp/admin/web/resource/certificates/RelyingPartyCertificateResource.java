package no.idporten.eudiw.rp.admin.web.resource.certificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummary;
import tools.jackson.databind.annotation.JsonDeserialize;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyCertificateResource(
    @JsonProperty(value = "certificate", required = true)
    @JsonDeserialize(using = X509CertificateJsonDeserializer.class)
    @JsonSerialize(using = X509CertificateJsonSerializer.class)
    X509Certificate certificate,
    @JsonProperty(value = "id", required = true)
    UUID id,
    @JsonProperty("entitlement")
    String entitlement
) {

    private String formatX500PrincipalName(X500Principal x500Principal) {
        return x500Principal.getName(
                X500Principal.RFC1779,
                Map.of("2.5.4.97", "organizationIdentifier"));
    }

    public RelyingPartyCertificateSummary toSummary() {
        return new RelyingPartyCertificateSummary(
            this.entitlement,
            this.certificate.getSerialNumber(),
            formatX500PrincipalName(this.certificate.getSubjectX500Principal()),
            formatX500PrincipalName(this.certificate.getIssuerX500Principal()),
            this.certificate.getNotBefore().toInstant().toEpochMilli(),
            this.certificate.getNotAfter().toInstant().toEpochMilli(),
            this.id
        );
    }
}

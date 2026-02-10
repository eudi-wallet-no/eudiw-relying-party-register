package no.idporten.eudiw.rp.admin.web.resource.certificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.security.cert.X509Certificate;
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
    String entitlement,
    @JsonProperty("revocation_status")
    int revocationStatus
) { }

package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.eudiw.rp.register.api.v1.resource.certificates.X509CertificateJsonDeserializer;
import no.eudiw.rp.register.api.v1.resource.certificates.X509CertificateJsonSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.security.cert.X509Certificate;
import java.util.UUID;

public record CertificateResponse(
    @JsonSerialize(using = X509CertificateJsonSerializer.class)
    @JsonDeserialize(using = X509CertificateJsonDeserializer.class)
    X509Certificate certificate,
    String entitlement,
    UUID id,
    @JsonProperty("revocation_status") Integer revocationStatus
) { }

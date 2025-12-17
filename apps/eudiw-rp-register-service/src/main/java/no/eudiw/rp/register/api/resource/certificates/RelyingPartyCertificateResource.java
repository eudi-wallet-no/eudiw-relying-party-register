package no.eudiw.rp.register.api.resource.certificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.security.cert.X509Certificate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyCertificateResource(
    @JsonProperty(value = "certificate", required = true)
    @JsonSerialize(using = X509CertificateJsonSerializer.class)
    @JsonDeserialize(using = X509CertificateJsonDeserializer.class)
    X509Certificate certificate,
    @JsonProperty("entitlement")
    String entitlement,
    @JsonProperty(value = "id")
    UUID id
) { }

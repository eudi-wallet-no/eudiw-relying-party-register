package no.eudiw.rp.register.api.resource.accesscertificates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyAccessCertificatesResource(
    @Valid
    @NotNull(message = "null_certificates")
    @JsonProperty(value = "certificates", required = true)
    List<RelyingPartyAccessCertificateResource> certificates
) { }

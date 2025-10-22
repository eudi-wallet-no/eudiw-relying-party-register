package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    String entitlement,

    @JsonProperty("display_name")
    String displayName,

    @Valid
    @JsonProperty(value = "certificates")
    List<RelyingPartyCertificateResource> certificates
) { }

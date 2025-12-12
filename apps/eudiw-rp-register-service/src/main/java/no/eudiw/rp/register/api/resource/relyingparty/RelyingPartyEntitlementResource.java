package no.eudiw.rp.register.api.resource.relyingparty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementResource(
    @JsonProperty(value = "entitlement", required = true)
    String entitlement,

    @JsonProperty("display_name")
    String displayName,

    @JsonProperty("credential_issuer_url")
    String credentialIssuerUrl,

    @Valid
    @JsonProperty(value = "certificates")
    List<RelyingPartyCertificateResource> certificates
) { }

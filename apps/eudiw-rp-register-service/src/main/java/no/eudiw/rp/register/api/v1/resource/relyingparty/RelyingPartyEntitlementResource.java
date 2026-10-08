package no.eudiw.rp.register.api.v1.resource.relyingparty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyEntitlementResource(
    @JsonProperty(value = "entitlement", required = true)
    String entitlement,

    @JsonProperty("display_name")
    String displayName,

    @JsonProperty("credential_issuer_url")
    String credentialIssuerUrl
) { }

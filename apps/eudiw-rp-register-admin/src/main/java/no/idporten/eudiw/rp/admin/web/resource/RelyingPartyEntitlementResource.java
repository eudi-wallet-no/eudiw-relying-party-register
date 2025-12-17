package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RelyingPartyEntitlementResource(
    @JsonProperty("entitlement")
    String entitlement,

    @JsonProperty("display_name")
    String displayName,

    @JsonProperty("credential_issuer_url")
    String credentialIssuerUrl
) {
    public RelyingPartyEntitlementResource(String entitlement) {
        this(entitlement, entitlement, null);
    }
}

package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;

import java.util.List;
import java.util.UUID;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelyingPartyResource(

    @JsonProperty(value = "id", required = true)
    UUID id,

    @JsonProperty(value = "orgno", required = true)
    String orgno,

    @JsonProperty(value = "organizationName", required = true)
    String orgName,

    @JsonProperty(value = "serviceTradeName", required = true)
    String tradeName,

    @JsonProperty("isPSB")
    Boolean publicSector,

    @JsonProperty(value = "relyingPartyEntitlements", required = true)
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

    @JsonProperty(value = "relyingPartyEaas", required = true)
    List<RelyingPartyEaaResource> relyingPartyEaas,

    @JsonProperty(value = "createdMs", required = true)
    long createdMs,

    @JsonProperty(value = "lastUpdatedMs", required = true)
    long lastUpdatedMs
) { }

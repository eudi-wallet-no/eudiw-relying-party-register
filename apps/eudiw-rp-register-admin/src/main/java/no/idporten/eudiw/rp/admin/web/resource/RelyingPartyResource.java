package no.idporten.eudiw.rp.admin.web.resource;

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

    @JsonProperty(value = "org_nr", required = true)
    String orgno,

    @JsonProperty(value = "name", required = true)
    String name,

    @JsonProperty(value = "public_sector", required = true)
    boolean publicSector,

    @JsonProperty(value = "relying_party_entitlements", required = true)
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

    @JsonProperty(value = "relying_party_eaas", required = true)
    List<RelyingPartyEaaResource> relyingPartyEaas,

    @JsonProperty(value = "created_ms", required = true)
    long createdMs,

    @JsonProperty(value = "last_updated_ms", required = true)
    long lastUpdatedMs,

    @JsonProperty(value = "active", required = true)
    boolean active
) { }

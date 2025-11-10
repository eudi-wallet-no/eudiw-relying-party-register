package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;

import java.util.List;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateRelyingPartyResource(
    @JsonProperty(value = "org_nr", required = true)
    String orgno,

    @JsonProperty(value = "name", required = true)
    String name,

    @JsonProperty(value = "public_sector", required = true)
    boolean publicSector,

    @JsonProperty("relying_party_entitlements")
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

    @JsonProperty("relying_party_eaas")
    List<RelyingPartyEaaResource> relyingPartyEaas
) { }

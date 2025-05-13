package no.idporten.eudiw.rp.admin.service.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;

import java.util.List;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record EditRelyingPartyResource(

        @JsonProperty(value = "name")
        String name,

        @JsonProperty(value = "public_sector")
        boolean publicSector,

        @JsonProperty("relying_party_entitlements")
        List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

        @JsonProperty("relying_party_eaas")
        List<RelyingPartyEaaResource> relyingPartyEaas,

        @JsonProperty(value = "active", required = true)
        boolean active
) { }
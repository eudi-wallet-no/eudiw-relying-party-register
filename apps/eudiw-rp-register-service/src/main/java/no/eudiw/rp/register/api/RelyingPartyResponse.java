package no.eudiw.rp.register.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.eudiw.rp.register.data.entity.RelyingParty;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartyResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty(value = "org_nr", required = true)
    private String orgNr;

    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty(value = "public_sector", required = true)
    private boolean publicSector;

    @JsonProperty("relying_party_entitlements")
    private List<RelyingPartyEntitlementResource> relyingPartyEntitlements;

    @JsonProperty("relying_party_eaas")
    private List<RelyingPartyEaaResource> relyingPartyEaas;

    @JsonProperty(value = "created_ms", required = false)
    private long createdMs;

    @JsonProperty(value = "last_updated_ms", required = false)
    private long lastUpdatedMs;

    @JsonProperty(value = "active", required = true)
    private boolean active;

    public RelyingPartyResponse(RelyingParty relyingParty) {
        this.id = relyingParty.getId();
        this.orgNr = relyingParty.getOrgno();
        this.name = relyingParty.getName();
        this.publicSector = relyingParty.getPublicSector();
        this.createdMs = relyingParty.getCreatedMs();
        this.lastUpdatedMs = relyingParty.getLastUpdatedMs();
        this.active = relyingParty.isActive();

        if (relyingParty.getRelyingPartyEntitlements() != null && !relyingParty.getRelyingPartyEntitlements().isEmpty()) {
            this.relyingPartyEntitlements = relyingParty.getRelyingPartyEntitlements()
                    .stream()
                    .map(entitlement -> new RelyingPartyEntitlementResource(
                            entitlement.getId(),
                            entitlement.getEntitlement()))
                    .collect(Collectors.toList());
        }

        if (relyingParty.getRelyingPartyEaas() != null && !relyingParty.getRelyingPartyEaas().isEmpty()) {
            this.relyingPartyEaas = relyingParty.getRelyingPartyEaas()
                    .stream()
                    .map(eaa -> new RelyingPartyEaaResource(
                            eaa.getId(),
                            eaa.getNamespace(),
                            eaa.getIntent()))
                    .collect(Collectors.toList());
        }
    }
}

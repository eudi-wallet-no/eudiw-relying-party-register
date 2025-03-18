package no.eudiw.rp.register.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartyResource {

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
}

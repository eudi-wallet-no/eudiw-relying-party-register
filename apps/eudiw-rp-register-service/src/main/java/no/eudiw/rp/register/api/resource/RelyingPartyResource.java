package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.eudiw.rp.register.validation.NameConstraint;
import no.eudiw.rp.register.validation.SaneStringConstraint;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartyResource {

    @NotNull(message = "null_id")
    @JsonProperty("id")
    private UUID id;

    @NotNull(message = "null_orgno")
    @JsonProperty(value = "org_nr", required = true)
    private String orgNr;

    @SaneStringConstraint
    @NameConstraint
    @NotNull(message = "null_name")
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty(value = "public_sector", required = true)
    private boolean publicSector;

    @Valid
    @NotEmpty(message = "empty_entitlements")
    @JsonProperty("relying_party_entitlements")
    private List<RelyingPartyEntitlementResource> relyingPartyEntitlements;

    @Valid
    @NotNull(message = "null_eaas")
    @JsonProperty("relying_party_eaas")
    private List<RelyingPartyEaaResource> relyingPartyEaas;

    @JsonProperty(value = "created_ms", required = false)
    private long createdMs;

    @JsonProperty(value = "last_updated_ms", required = false)
    private long lastUpdatedMs;

    @JsonProperty(value = "active", required = true)
    private boolean active;
}

package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.eudiw.rp.register.validation.NameConstraint;
import no.eudiw.rp.register.validation.SaneStringConstraint;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditRelyingPartyResource {

    @SaneStringConstraint
    @NameConstraint
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty(value = "public_sector", required = true)
    private boolean publicSector;

    @Valid
    @NotNull(message = "null_entitlements")
    @JsonProperty("relying_party_entitlements")
    private List<RelyingPartyEntitlementResource> relyingPartyEntitlements;

    @Valid
    @NotNull(message = "null_eaas")
    @JsonProperty("relying_party_eaas")
    private List<RelyingPartyEaaResource> relyingPartyEaas;

    @JsonProperty(value = "active", required = true)
    private boolean active;
}

package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.eudiw.rp.register.validation.SaneStringConstraint;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartyEntitlementResource {

    @SaneStringConstraint(message = "unsane_entitlement")
    @NotBlank(message = "blank_entitlement")
    @JsonProperty("entitlement")
    private String entitlement;
}

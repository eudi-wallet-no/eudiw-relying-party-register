package no.eudiw.rp.register.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelyingPartyEntitlementResource {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("entitlement")
    private String entitlement;
}

package no.eudiw.rp.register.api.resource.relyingparty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.With;
import no.eudiw.rp.register.validation.SaneStringConstraint;

import java.util.List;

@With
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EditRelyingPartyResource(

    @SaneStringConstraint
    @NotBlank(message = "blank_name")
    @JsonProperty(value = "name", required = true)
    String tradeName,

    @Valid
    @NotNull(message = "null_entitlements")
    @JsonProperty("relying_party_entitlements")
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

    @Valid
    @NotNull(message = "null_eaas")
    @JsonProperty("relying_party_eaas")
    List<RelyingPartyEaaResource> relyingPartyEaas,

    @JsonProperty(value = "active", required = true)
    boolean active
) { }

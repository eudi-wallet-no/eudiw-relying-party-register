package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.With;
import no.eudiw.rp.register.validation.NameConstraint;
import no.eudiw.rp.register.validation.SaneStringConstraint;

import no.idporten.validators.orgnr.Orgnr;

import java.util.List;

@With
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CreateRelyingPartyResource(
    @Orgnr
    @NotNull(message = "null_orgno")
    @JsonProperty(value = "org_nr", required = true)
    String orgNr,

    @SaneStringConstraint
    @NameConstraint
    @NotNull(message = "null_name")
    @JsonProperty(value = "name", required = true)
    String name,

    @JsonProperty(value = "public_sector", required = true)
    boolean publicSector,

    @Valid
    @NotEmpty(message = "empty_entitlements")
    @JsonProperty("relying_party_entitlements")
    List<RelyingPartyEntitlementResource> relyingPartyEntitlements,

    @Valid
    @NotNull(message = "null_eaas")
    @JsonProperty("relying_party_eaas")
    List<RelyingPartyEaaResource> relyingPartyEaas
) { }

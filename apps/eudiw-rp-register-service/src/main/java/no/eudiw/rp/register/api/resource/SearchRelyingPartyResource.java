package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import no.idporten.validators.orgnr.Orgnr;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchRelyingPartyResource(
    @Orgnr
    @NotNull(message = "null_orgno")
    @JsonProperty("orgno")
    String orgno,
    @JsonProperty("public_sector")
    Boolean publicSector,
    @JsonProperty("include_inactive")
    Boolean includeInactive
) { }

package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchRelyingPartyResource(
    @JsonProperty("orgno") String orgno,
    @JsonProperty("public_sector") Boolean publicSector,
    @JsonProperty("include_inactive") Boolean includeInactive
) {
    public SearchRelyingPartyResource(
        String orgno,
        Boolean publicSector,
        Boolean includeInactive) {
        this.orgno = orgno;
        this.publicSector = publicSector;
        this.includeInactive = includeInactive;
    }
}

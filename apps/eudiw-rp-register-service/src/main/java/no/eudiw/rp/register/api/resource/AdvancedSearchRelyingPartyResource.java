package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.With;
import no.eudiw.rp.register.validation.SaneStringConstraint;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdvancedSearchRelyingPartyResource(
    // in order to support prefix searching, we do not require valid orgno here.
    @Pattern(regexp = "\\d{0,9}", message = "invalid_search_orgno")
    @JsonProperty("orgno")
    String orgno,

    @SaneStringConstraint
    @JsonProperty("name")
    String name,

    @JsonProperty("public_sector")
    Boolean publicSector,

    @JsonProperty(value = "include_inactive", defaultValue = "false")
    Boolean includeInactive
) {
    public static AdvancedSearchRelyingPartyResource empty() {
        return new AdvancedSearchRelyingPartyResource(null, null, null, false);
    }
}

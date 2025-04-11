package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.With;
import no.eudiw.rp.register.validation.SaneStringConstraint;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchRelyingPartyResource(
    @JsonProperty(value = "search_term", required = true)
    @SaneStringConstraint
    @NotNull(message = "null_search_term")
    String searchTerm,

    @JsonProperty(value = "include_inactive", required = true)
    boolean includeInactive
) {
    public static SearchRelyingPartyResource empty() {
        return new SearchRelyingPartyResource("", false);
    }
}

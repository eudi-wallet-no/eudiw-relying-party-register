package no.eudiw.rp.register.api.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import no.eudiw.rp.register.validation.SaneStringConstraint;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchRelyingPartyResource {
    @JsonProperty(value = "search_term", required = true)
    @SaneStringConstraint
    @NotNull(message = "null_search_term")
    private String searchTerm = "";

    @JsonProperty(value = "include_inactive", required = true)
    private boolean includeInactive = false;

    @JsonProperty(value = "required_entitlements")
    private List<RelyingPartyEntitlementResource> requiredEntitlements = List.of();

    public SearchRelyingPartyResource(String searchTerm) {
        this();
        this.searchTerm = searchTerm;
    }
}

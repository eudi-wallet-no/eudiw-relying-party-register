package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchRelyingPartyResource(
    @JsonProperty("search_term")
    String searchTerm,
    @JsonProperty("include_inactive")
    boolean includeInactive,
    @JsonProperty("required_entitlements")
    List<RelyingPartyEntitlementResource> requiredEntitlements,
    @JsonProperty(value = "page")
    Integer page,
    @JsonProperty(value = "page_size")
    Integer pageSize
) {
    public static SearchRelyingPartyResource updatePage(SearchRelyingPartyResource oldResource, Integer page) {
        return new SearchRelyingPartyResource(
            oldResource.searchTerm(),
            oldResource.includeInactive(),
            oldResource.requiredEntitlements,
            page,
            oldResource.pageSize
        );
    }

}

package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@With
public record SearchRelyingPartyResource(
    @JsonProperty("search_term")
    String searchTerm,
    @JsonProperty("include_inactive")
    boolean includeInactive,
    @JsonProperty("required_entitlements")
    List<RelyingPartyEntitlementResource> requiredEntitlements,
    @JsonProperty(value = "page")
    int page,
    @JsonProperty(value = "page_size")
    int pageSize
) {
    public SearchRelyingPartyResource(SearchForm searchForm, int page, int pageSize) {
        this(searchForm.searchTerm(),
             searchForm.includeInactive(),
             searchForm.requiredEntitlements()
                       .stream()
                       .map(RelyingPartyEntitlementFormField::toResource)
                       .toList(),
             page,
             pageSize);
    }
}

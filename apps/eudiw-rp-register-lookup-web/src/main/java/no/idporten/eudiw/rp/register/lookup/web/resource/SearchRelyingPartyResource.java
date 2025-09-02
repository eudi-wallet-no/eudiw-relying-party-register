package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import no.idporten.eudiw.rp.register.lookup.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Accessors(fluent = true)
public class SearchRelyingPartyResource {
    @JsonProperty("search_term")
    private String searchTerm;
    @JsonProperty("include_inactive")
    private final boolean includeInactive = false;
    @JsonProperty("required_entitlements")
    private List<RelyingPartyEntitlementResource> requiredEntitlements;
    @JsonProperty(value = "page")
    private int page;
    @JsonProperty(value = "page_size")
    private int pageSize;

    public SearchRelyingPartyResource(SearchForm searchForm, int page, int pageSize) {
        this(searchForm.searchTerm(),
                searchForm.requiredEntitlements()
                        .stream()
                        .map(RelyingPartyEntitlementFormField::toResource)
                        .toList(),
                page,
                pageSize);
    }
}

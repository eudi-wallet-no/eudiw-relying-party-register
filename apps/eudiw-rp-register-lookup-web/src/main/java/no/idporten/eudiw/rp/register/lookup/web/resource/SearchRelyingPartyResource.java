package no.idporten.eudiw.rp.register.lookup.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import no.idporten.eudiw.rp.register.lookup.web.search.resultsview.RelyingPartyOrdering;
import no.idporten.eudiw.rp.register.lookup.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@With
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
@Accessors(fluent = true)
public class SearchRelyingPartyResource {
    public final static int DEFAULT_PAGE_SIZE = 25;
    public final static RelyingPartyOrdering DEFAULT_ORDERING = RelyingPartyOrdering.NAME_ASC;

    @JsonProperty("search_term")
    private String searchTerm = "";

    @JsonProperty("hide_synthetic_orgnos")
    private boolean hideSyntheticOrgnos;

    @JsonProperty("required_entitlements")
    @NotNull
    private List<RelyingPartyEntitlementResource> requiredEntitlements = List.of();

    @JsonProperty(value = "page")
    private int page = 0;

    @JsonProperty(value = "page_size")
    private int pageSize = DEFAULT_PAGE_SIZE;

    @JsonProperty(value = "order_by")
    @NotNull
    private RelyingPartyOrdering ordering = DEFAULT_ORDERING;

    public SearchRelyingPartyResource(SearchForm searchForm) {
        this();
        this.searchTerm = searchForm.getSearchTerm();
        this.hideSyntheticOrgnos = searchForm.isHideSyntheticOrgnos();
        this.requiredEntitlements =
            searchForm.getRequiredEntitlements()
                      .stream()
                      .map(RelyingPartyEntitlementFormField::toResource)
                      .toList();
    }
}

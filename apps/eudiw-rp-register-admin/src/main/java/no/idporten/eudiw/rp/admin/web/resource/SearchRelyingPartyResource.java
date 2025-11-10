package no.idporten.eudiw.rp.admin.web.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@With
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
public class SearchRelyingPartyResource {
    public final static int DEFAULT_PAGE_SIZE = 25;
    public final static RelyingPartyOrdering DEFAULT_ORDERING = RelyingPartyOrdering.UNSORTED;

    @JsonProperty("search_term")
    private String searchTerm = "";

    @JsonProperty("include_inactive")
    private boolean includeInactive = false;

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
        this.searchTerm = searchForm.searchTerm();
        this.includeInactive = searchForm.includeInactive();
        this.requiredEntitlements =
            searchForm.requiredEntitlements()
                      .stream()
                      .map(RelyingPartyEntitlementFormField::toResource)
                      .toList();
    }
}

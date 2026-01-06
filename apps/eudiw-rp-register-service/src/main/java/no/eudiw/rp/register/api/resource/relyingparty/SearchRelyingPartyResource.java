package no.eudiw.rp.register.api.resource.relyingparty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import no.eudiw.rp.register.validation.SaneStringConstraint;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@With
public class SearchRelyingPartyResource {
    public static final int DEFAULT_PAGE_SIZE = 25;

    @JsonProperty(value = "search_term", required = true)
    @SaneStringConstraint
    @NotNull(message = "null_search_term")
    private String searchTerm = "";

    @JsonProperty(value = "include_inactive", required = true)
    private boolean includeInactive = false;

    @JsonProperty(value = "hide_synthetic_orgnos")
    private boolean hideSyntheticOrgnos = false;

    @JsonProperty(value = "required_entitlements")
    @NotNull
    private List<String> requiredEntitlements = new ArrayList<>();

    @JsonProperty(value = "page")
    @Min(value = 0, message = "invalid_page_index")
    private int page = 0;

    @JsonProperty(value = "page_size")
    @Min(value = 0, message = "invalid_page_size")
    private int pageSize = DEFAULT_PAGE_SIZE;

    @AssertTrue(message = "invalid_page_offset")
    @SuppressWarnings("unused") // used by jakarta
    private boolean assertPageOffsetIsInBounds() {
        return (long) page * pageSize <= Integer.MAX_VALUE;
    }

    @JsonProperty("order_by")
    private String sortKey;

    public SearchRelyingPartyResource(String searchTerm) {
        this();
        this.searchTerm = searchTerm;
    }
}

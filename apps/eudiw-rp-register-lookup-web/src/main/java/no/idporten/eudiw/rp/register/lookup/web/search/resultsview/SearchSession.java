package no.idporten.eudiw.rp.register.lookup.web.search.resultsview;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.*;
import java.util.stream.IntStream;

@Component("searchSession")
@SessionScope
@Getter
@RequiredArgsConstructor
public class SearchSession {

    @Getter(AccessLevel.NONE)
    private final RelyingPartiesService relyingPartiesService;

    @Setter
    private SearchForm lastSearchForm = SearchForm.empty();

    @Setter
    private String ordering = SearchRelyingPartyResource.DEFAULT_ORDERING;

    private int numPages = 0;
    private int currentPageIdx = 0;
    private int pageSize = SearchRelyingPartyResource.DEFAULT_PAGE_SIZE;

    public List<RelyingPartyResource> doFreshSearch(SearchForm searchForm) {
        return this.doSearch(searchForm, 0, this.pageSize, this.ordering);
    }

    public List<RelyingPartyResource> refreshSearch() {
        return this.doSearch(this.lastSearchForm,
                             this.currentPageIdx,
                             this.pageSize,
                             this.ordering);
    }

    public void setCurrentPageIdx(int nextPageIdx) {
        this.currentPageIdx = Math.max(Math.min(nextPageIdx, numPages - 1), 0);
    }

    public int[] getPaginationWindow() {
        int windowSize = Math.min(5, numPages);
        int windowRadius = windowSize / 2;
        int lo = currentPageIdx + windowRadius < numPages
                     ? Math.max(0, currentPageIdx - windowRadius)
                     : numPages - windowSize;
        int hi = lo + windowSize;
        return IntStream.range(lo, hi).map(k -> k + 1).toArray();
    }

    private List<RelyingPartyResource> doSearch(
        SearchForm searchForm,
        int pageIdx,
        int pageSize,
        String ordering) {
        SearchRelyingPartyResource searchResource =
            searchForm.toResource()
                .withPage(pageIdx)
                .withPageSize(pageSize)
                .withOrdering(ordering);

        PagedResponse<RelyingPartyResource> searchResult =
            relyingPartiesService.search(searchResource);

        this.numPages = Math.toIntExact(searchResult.page().totalPages());
        this.currentPageIdx = Math.toIntExact(searchResult.page().number());

        this.setLastSearchForm(searchForm);

        return searchResult.content();
    }
}

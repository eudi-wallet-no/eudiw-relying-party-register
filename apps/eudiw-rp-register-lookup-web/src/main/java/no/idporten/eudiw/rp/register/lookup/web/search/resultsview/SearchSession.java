package no.idporten.eudiw.rp.register.lookup.web.search.resultsview;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.PagedResponse;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component("searchSession")
@SessionScope
@Getter
@RequiredArgsConstructor
public class SearchSession {

    public static final int DEFAULT_PAGE_SIZE = 25;

    @Getter(AccessLevel.NONE)
    private HashMap<UUID, RelyingPartyResource> currentSearchResult = new HashMap<>();

    @Getter(AccessLevel.NONE)
    private final RelyingPartiesService relyingPartiesService;

    @Setter
    private SearchForm lastSearchForm = SearchForm.empty();

    private RelyingPartiesViewOrdering ordering = RelyingPartiesViewOrdering.NAME_ASC;

    private int numPages = 0;
    private int currentPageIdx = 0;
    private int pageSize = DEFAULT_PAGE_SIZE;

    private boolean initialized = false;

    public void doFreshSearch(SearchForm searchForm) {
        this.doSearch(searchForm, 0, this.pageSize);
    }

    public List<RelyingPartyResource> getCurrentSearchResultsPage() {
        return new ArrayList<>(this.currentSearchResult.values());
    }

    public void setCurrentPageIdx(int nextPageIdx) {
        nextPageIdx = Math.max(Math.min(nextPageIdx, numPages - 1), 0);
        if (nextPageIdx != this.currentPageIdx) {
            this.doSearch(this.lastSearchForm, nextPageIdx, this.pageSize);
        }
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

    private void doSearch(SearchForm searchForm, int pageIdx, int pageSize) {
        SearchRelyingPartyResource searchResource =
            new SearchRelyingPartyResource(searchForm, pageIdx, pageSize);

        PagedResponse<RelyingPartyResource> searchResult =
            relyingPartiesService.search(searchResource);

        this.currentSearchResult =
            searchResult.content()
                        .stream()
                        .collect(relyingPartyLinkedMapCollector);

        this.numPages = Math.toIntExact(searchResult.page().totalPages());
        this.currentPageIdx = Math.toIntExact(searchResult.page().number());

        this.setLastSearchForm(searchForm);
        this.initialized = true;
    }

    private static final
    Collector<RelyingPartyResource, ?, LinkedHashMap<UUID, RelyingPartyResource>>
        relyingPartyLinkedMapCollector =
        Collectors.toMap(RelyingPartyResource::id,
                         rp -> rp,
                         (_, snd) -> snd,
                         LinkedHashMap::new);
}

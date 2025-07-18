package no.idporten.eudiw.rp.admin.web.search.resultsview;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component("relyingPartiesView")
@SessionScope
@Getter
public class RelyingPartiesView {

    @Getter(AccessLevel.NONE)
    private HashMap<UUID, RelyingPartyResource> relyingParties;

    @Getter(AccessLevel.NONE)
    private final RelyingPartiesService relyingPartiesService;

    private int numPages;
    private int currentPageIdx;
    private int pageSize;

    private RelyingPartiesViewOrdering ordering;

    private boolean initialized;

    @Setter
    private SearchForm lastSearchForm = SearchForm.empty();

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int PAGINATION_WINDOW_SIZE = 5;

    private static final
    Collector<RelyingPartyResource, ?, LinkedHashMap<UUID, RelyingPartyResource>>
        relyingPartyLinkedMapCollector =
        Collectors.toMap(RelyingPartyResource::id,
                         rp -> rp,
                         (_, snd) -> snd,
                         LinkedHashMap::new);

    public RelyingPartiesView(RelyingPartiesService relyingPartiesService) {
        this.relyingPartiesService = relyingPartiesService;

        this.ordering = RelyingPartiesViewOrdering.NAME_ASC;
        this.relyingParties = new HashMap<>();

        this.numPages = 0;
        this.currentPageIdx = 0;
        this.pageSize = DEFAULT_PAGE_SIZE;

        this.initialized = false;
    }

    public void setCurrentPageIdx(int currentPageIdx) {
        this.currentPageIdx = Math.max(Math.min(currentPageIdx, numPages - 1), 0);
    }

    public void doSearch(SearchRelyingPartyResource searchResource) {
        RelyingPartiesResource searchResult =
            this.relyingPartiesService.search(searchResource);

        this.ordering = RelyingPartiesViewOrdering.NAME_ASC;
        this.relyingParties =
            searchResult.relyingParties()
                        .stream()
                        .sorted(this.ordering.toComparator())
                        .collect(relyingPartyLinkedMapCollector);

        this.numPages = Math.ceilDiv(this.relyingParties.size(), DEFAULT_PAGE_SIZE);
        this.currentPageIdx = 0;

        this.initialized = true;
    }

    public RelyingPartyResource edit(UUID id, EditRelyingPartyResource editResource) {
        RelyingPartyResource edited = this.relyingPartiesService.edit(id, editResource);

        // if RP is in current search results, reflect the changes there
        if (this.relyingParties.containsKey(id)) {
            this.relyingParties.put(id, edited);
        }
        return edited;
    }

    public void setOrdering(RelyingPartiesViewOrdering ordering) {
        if (ordering != this.ordering) {
            this.relyingParties =
                this.relyingParties
                    .values()
                    .stream()
                    .sorted(ordering.toComparator())
                    .collect(relyingPartyLinkedMapCollector);
            this.ordering = ordering;
        }
    }

    public List<RelyingPartyResource> getCurrentResultsPage() {
        return this.relyingParties
                   .values()
                   .stream()
                   .skip((long) currentPageIdx * pageSize)
                   .limit(pageSize)
                   .toList();
    }
    public int[] getPaginationWindow() {
        int windowSize = Math.min(PAGINATION_WINDOW_SIZE, numPages);
        int windowRadius = windowSize / 2;
        int lo = currentPageIdx + windowRadius < numPages
                     ? Math.max(0, currentPageIdx - windowRadius)
                     : numPages - windowSize;
        int hi = lo + windowSize;
        return IntStream.range(lo, hi).map(k -> k + 1).toArray();
    }
}

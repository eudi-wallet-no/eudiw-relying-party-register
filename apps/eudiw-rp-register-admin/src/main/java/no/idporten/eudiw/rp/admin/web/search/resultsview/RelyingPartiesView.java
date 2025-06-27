package no.idporten.eudiw.rp.admin.web.search.resultsview;

import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component("relyingPartiesView")
@SessionScope
public class RelyingPartiesView {

    private HashMap<UUID, RelyingPartyResource> relyingParties;

    private List<RelyingPartyResource> currentView;

    private final RelyingPartiesService relyingPartiesService;

    @Getter
    private int numPages;

    @Getter
    private int currentPageIdx;

    @Getter
    private int pageSize;

    private boolean initialized;

    @Getter
    @Setter
    private SearchForm lastSearchForm = SearchForm.empty();

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int PAGINATION_WINDOW_SIZE = 5;

    public RelyingPartiesView(RelyingPartiesService relyingPartiesService) {
        this.relyingPartiesService = relyingPartiesService;

        this.relyingParties = new HashMap<>();
        this.currentView = new ArrayList<>();

        this.numPages = 0;
        this.currentPageIdx = 0;
        this.pageSize = DEFAULT_PAGE_SIZE;

        this.initialized = false;
    }

    public void setCurrentPageIdx(int currentPageIdx) {
        this.currentPageIdx = Math.max(Math.min(currentPageIdx, numPages - 1), 0);
    }
    public boolean hasResults() {
        return this.initialized;
    }

    public void doSearch(SearchRelyingPartyResource searchResource) {
        RelyingPartiesResource searchResult =
            this.relyingPartiesService.search(searchResource);
        this.relyingParties =
            searchResult.relyingParties().stream().collect(
                Collectors.toMap(RelyingPartyResource::id,
                                 Function.identity(),
                                 (_, b) -> b,
                                 HashMap::new));

        this.currentView = searchResult.relyingParties();
        this.setViewSpec(ResultsViewSpecification.defaultView());

        this.numPages = Math.ceilDiv(this.relyingParties.size(), DEFAULT_PAGE_SIZE);
        this.currentPageIdx = 0;

        this.initialized = true;
    }

    private void setViewSpec(ResultsViewSpecification viewSpec) {
        Predicate<RelyingPartyResource> combinedFilter =
            viewSpec.filters().stream().reduce(_ -> true, Predicate::and);
        this.currentView =
            this.relyingParties
                   .values()
                   .stream()
                   .filter(combinedFilter)
                   .sorted(viewSpec.ordering())
                   .toList();
    }

    public RelyingPartyResource get(UUID id) {
        if (this.relyingParties.containsKey(id)) {
            return this.relyingParties.get(id);
        }

        RelyingPartyResource rp = this.relyingPartiesService.get(id);
        this.relyingParties.put(id, rp);
        return rp;
    }
    public RelyingPartyResource edit(UUID id, EditRelyingPartyResource editResource) {
        RelyingPartyResource edited = this.relyingPartiesService.edit(id, editResource);
        this.relyingParties.put(id, edited);
        return edited;
    }
    public RelyingPartyResource create(CreateRelyingPartyResource createResource) {
        RelyingPartyResource created = this.relyingPartiesService.create(createResource);
        this.relyingParties.put(created.id(), created);
        return created;
    }

    public List<RelyingPartyResource> getCurrentResultsPage() {
        return this.currentView
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

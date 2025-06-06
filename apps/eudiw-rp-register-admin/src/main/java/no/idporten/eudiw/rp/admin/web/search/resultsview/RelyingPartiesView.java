package no.idporten.eudiw.rp.admin.web.search.resultsview;

import lombok.Setter;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component("relyingPartiesView")
@SessionScope
public class RelyingPartiesView {

    private HashMap<UUID, RelyingPartyResource> relyingParties;

    @Setter
    private ResultsViewSpecification viewSpec;

    private final RelyingPartiesService relyingPartiesService;

    public RelyingPartiesView(RelyingPartiesService relyingPartiesService) {
        this.relyingPartiesService = relyingPartiesService;
        this.relyingParties = new HashMap<>();
        this.viewSpec = ResultsViewSpecification.defaultView();
    }

    public RelyingPartiesResource doSearch(SearchRelyingPartyResource searchResource) {
        RelyingPartiesResource searchResult = this.relyingPartiesService.search(searchResource);
        this.relyingParties =
            searchResult.relyingParties().stream().collect(
                Collectors.toMap(RelyingPartyResource::id,
                                 Function.identity(),
                                 (_, b) -> b,
                                 HashMap::new));
        this.viewSpec = ResultsViewSpecification.defaultView();
        return searchResult;
    }

    @SuppressWarnings("unused") // used in Thymeleaf template
    public List<RelyingPartyResource> applyViewSpec() {
        Predicate<RelyingPartyResource> combinedFilter =
            viewSpec.filters().stream().reduce(_ -> true, Predicate::and);
        return relyingParties
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
}

package no.idporten.eudiw.rp.register.lookup.web.searchresults;

import lombok.AllArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.searchresults.viewing.ResultsViewSpecification;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RelyingPartiesView {

    private final HashMap<UUID, RelyingPartyResource> relyingParties;

    @Setter
    private ResultsViewSpecification viewSpec;

    public static RelyingPartiesView fromResource(RelyingPartiesResource resource) {
        HashMap<UUID, RelyingPartyResource> asHashMap =
            resource.relyingParties().stream().collect(
                Collectors.toMap(RelyingPartyResource::id,
                                 Function.identity(),
                                 (_, b) -> b,
                                 HashMap::new));
        return new RelyingPartiesView(asHashMap, ResultsViewSpecification.defaultView());
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

    public boolean exists(UUID id) {
        return this.relyingParties.containsKey(id);
    }
    public RelyingPartyResource get(UUID id) {
        return this.relyingParties.get(id);
    }
}

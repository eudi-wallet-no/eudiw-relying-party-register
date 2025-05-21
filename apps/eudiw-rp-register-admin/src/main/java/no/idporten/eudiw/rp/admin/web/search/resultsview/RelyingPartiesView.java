package no.idporten.eudiw.rp.admin.web.search.resultsview;

import lombok.AllArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
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
    public static RelyingPartiesView empty() {
        return new RelyingPartiesView(new HashMap<>(), ResultsViewSpecification.defaultView());
    }
    public boolean exists(UUID id) {
        return this.relyingParties.containsKey(id);
    }
    public RelyingPartyResource get(UUID id) {
        return this.relyingParties.get(id);
    }
}

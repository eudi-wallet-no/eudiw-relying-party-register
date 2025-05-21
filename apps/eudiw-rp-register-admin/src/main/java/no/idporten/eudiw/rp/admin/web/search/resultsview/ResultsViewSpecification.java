package no.idporten.eudiw.rp.admin.web.search.resultsview;

import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public record ResultsViewSpecification(
    Comparator<RelyingPartyResource> ordering,
    Collection<Predicate<RelyingPartyResource>> filters
) {

    private static final Comparator<RelyingPartyResource> byNameAsc =
        Comparator.comparing(RelyingPartyResource::name);
    private static final Comparator<RelyingPartyResource> byOrgnoAsc =
        Comparator.comparing(RelyingPartyResource::orgno);
    private static final Comparator<RelyingPartyResource> byCreatedAsc =
        Comparator.comparing(RelyingPartyResource::createdMs);
    private static final Comparator<RelyingPartyResource> byLastUpdatedAsc =
        Comparator.comparing(RelyingPartyResource::lastUpdatedMs);

    private static Predicate<RelyingPartyResource> makeEntitlementPredicate(String s) {
        return rp -> rp.relyingPartyEntitlements()
                       .stream()
                       .map(RelyingPartyEntitlementResource::entitlement)
                       .anyMatch(ent -> ent.startsWith(s));
    }

    public static ResultsViewSpecification fromForm(ResultsViewSpecificationForm form) {
        Comparator<RelyingPartyResource> ordering =
            switch (form.ordering()) {
                case NAME_ASC -> byNameAsc;
                case ORGNO_ASC -> byOrgnoAsc;
                case CREATED_ASC -> byCreatedAsc;
                case LAST_UPDATED_ASC -> byLastUpdatedAsc;
            };

        Collection<Predicate<RelyingPartyResource>> entitlementFilters =
            Arrays.stream(form.entitlementFreeTextField()
                              .strip()
                              .split("[\\s,]+"))
                  .map(ResultsViewSpecification::makeEntitlementPredicate)
                  .toList();
        return new ResultsViewSpecification(ordering, entitlementFilters);
    }

    public static ResultsViewSpecification defaultView() {
        return new ResultsViewSpecification(byNameAsc, List.of(_ -> true));
    }
}

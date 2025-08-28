package no.idporten.eudiw.rp.register.lookup.web.search.resultsview;



import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;

import java.util.Comparator;

public enum RelyingPartiesViewOrdering {
    NAME_ASC,
    ORGNO_ASC,
    CREATED_ASC,
    LAST_UPDATED_ASC;

    public Comparator<RelyingPartyResource> toComparator() {
        return switch (this) {
            case NAME_ASC -> Comparator.comparing(RelyingPartyResource::name);
            case ORGNO_ASC -> Comparator.comparing(RelyingPartyResource::orgno);
            case CREATED_ASC -> Comparator.comparing(RelyingPartyResource::createdMs);
            case LAST_UPDATED_ASC -> Comparator.comparing(RelyingPartyResource::lastUpdatedMs);
        };
    }
}

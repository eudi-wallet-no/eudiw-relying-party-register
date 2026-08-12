package no.eudiw.rp.register.data;

import org.springframework.data.domain.Sort;

public class RelyingPartyOrdering {
    private RelyingPartyOrdering() { }

    public static final String TRADE_NAME_KEY = "tradeName";
    public static final String ORGNO_KEY = "legalEntity.orgno";
    public static final String CREATED_MS_KEY = "createdMs";
    public static final String LAST_UPDATED_MS_KEY = "lastUpdatedMs";
    public static final String UNSORTED_KEY = "unsorted";

    public static Sort fromSortKey(String sortKey) {
        return switch (sortKey) {
            case "name", TRADE_NAME_KEY -> Sort.by(TRADE_NAME_KEY);
            case "orgno", ORGNO_KEY -> Sort.by(ORGNO_KEY);
            case CREATED_MS_KEY -> Sort.by(CREATED_MS_KEY);
            case LAST_UPDATED_MS_KEY -> Sort.by(LAST_UPDATED_MS_KEY);
            case null, default -> Sort.unsorted();
        };
    }
}

package no.idporten.eudiw.rp.register.lookup.web.searchresults;

import lombok.With;
import no.idporten.eudiw.rp.register.lookup.web.searchresults.viewing.Ordering;

@With
public record ResultsViewSpecificationForm(
    Ordering ordering,
    String entitlementFreeTextField
) {
    public ResultsViewSpecificationForm(Ordering ordering, String entitlementFreeTextField) {
        this.ordering = ordering;
        this.entitlementFreeTextField =
            String.join("\n", entitlementFreeTextField.split("[\\s,]+"));
    }

    public static ResultsViewSpecificationForm empty() {
        return new ResultsViewSpecificationForm(Ordering.NAME_ASC, "");
    }

    public static Ordering[] getOrderingOptions() {
        return Ordering.values();
    }
}

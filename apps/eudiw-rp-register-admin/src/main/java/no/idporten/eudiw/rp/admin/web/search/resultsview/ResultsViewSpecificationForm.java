package no.idporten.eudiw.rp.admin.web.search.resultsview;

import lombok.With;

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

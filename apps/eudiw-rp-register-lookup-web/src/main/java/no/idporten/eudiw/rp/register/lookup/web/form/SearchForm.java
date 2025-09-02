package no.idporten.eudiw.rp.register.lookup.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.With;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringValidator;

import java.util.ArrayList;
import java.util.List;

@With
public record SearchForm(
    @Size(max = 255, message = "søketerm må max være 255 tegn")
    @SaneStringConstraint(message =
        "søketerm får bare inneholde norske bokstaver, tall, mellemrom, og symbolene "
            + SaneStringValidator.ALLOWED_SYMBOLS,
                          nullable = false)
    String searchTerm,
    @NotNull
    List<RelyingPartyEntitlementFormField> requiredEntitlements
) {

    public static SearchForm empty() {
        return new SearchForm("", new ArrayList<>());
    }

    public SearchForm(
        String searchTerm,
        List<RelyingPartyEntitlementFormField> requiredEntitlements) {
        this.searchTerm = searchTerm.strip();
        this.requiredEntitlements =
            requiredEntitlements != null ? requiredEntitlements : new ArrayList<>();
    }

    public List<String> requiredEntitlementValues() {
        return requiredEntitlements
                .stream()
                .map(RelyingPartyEntitlementFormField::getEntitlement)
                .toList();
    }
}

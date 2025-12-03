package no.idporten.eudiw.rp.register.lookup.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringValidator;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SearchForm {
    @Size(max = 255, message = "søketerm må max være 255 tegn")
    @SaneStringConstraint(message =
        "søketerm får bare inneholde norske bokstaver, tall, mellemrom, og symbolene "
            + SaneStringValidator.ALLOWED_SYMBOLS,
                          nullable = false)

    private String searchTerm = "";
    private boolean hideSyntheticOrgnos = false;


    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm.strip();
    }

    @NotNull
    private List<RelyingPartyEntitlementFormField> requiredEntitlements = new ArrayList<>();

    public static SearchForm empty() {
        return new SearchForm();
    }


    public List<String> requiredEntitlementValues() {
        return requiredEntitlements
                .stream()
                .map(RelyingPartyEntitlementFormField::getEntitlement)
                .toList();
    }
}

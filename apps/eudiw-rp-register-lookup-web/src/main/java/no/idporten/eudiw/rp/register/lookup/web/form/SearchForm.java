package no.idporten.eudiw.rp.register.lookup.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.register.lookup.validation.SaneStringValidator;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;

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
    private List<String> requiredEntitlements = new ArrayList<>();

    public static SearchForm empty() {
        return new SearchForm();
    }

    public SearchRelyingPartyResource toResource() {
        return new SearchRelyingPartyResource()
                   .withSearchTerm(this.searchTerm)
                   .withHideSyntheticOrgnos(this.hideSyntheticOrgnos)
                   .withRequiredEntitlements(this.requiredEntitlements);
    }
}

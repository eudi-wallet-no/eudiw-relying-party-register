package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.With;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;

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
    boolean includeInactive,
    @NotNull
    List<RelyingPartyEntitlementFormField> requiredEntitlements,
    Integer page,
    Integer pageSize
) {

    public static Integer DEFAULT_PAGE_SIZE = 25;
    public static Integer DEFAULT_PAGE_NUMBER = 0;

    public static SearchForm empty() {
        return new SearchForm("", false, new ArrayList<>(), null, null);
    }

    public SearchForm(
        String searchTerm,
        boolean includeInactive,
        List<RelyingPartyEntitlementFormField> requiredEntitlements,
        Integer page,
        Integer pageSize
    ) {
        this.searchTerm = searchTerm.strip();
        this.includeInactive = includeInactive;
        this.requiredEntitlements = requiredEntitlements != null ? requiredEntitlements : new ArrayList<>();
        this.page = page != null ? page : DEFAULT_PAGE_NUMBER;
        this.pageSize = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
    }

    public SearchRelyingPartyResource toResource() {
        return new SearchRelyingPartyResource(
            this.searchTerm,
            this.includeInactive,
            this.requiredEntitlements
                .stream()
                .map(RelyingPartyEntitlementFormField::toResource)
                .toList(),
            this.page,
            this.pageSize);
    }

    public List<String> requiredEntitlementValues() {
        return requiredEntitlements
                   .stream()
                   .map(RelyingPartyEntitlementFormField::getEntitlement)
                   .toList();
    }
}

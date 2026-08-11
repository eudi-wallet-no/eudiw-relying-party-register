package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class BaseEditRelyingPartyForm {

    @SaneStringConstraint(message =
        "Ugyldig tenestenamn. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Tenestenamn må fyllast ut")
    private String tradeName = "";

    @Valid
    // @NotEmpty(message = "Brukerstedet må ha minst en entitlement")
    private List<RelyingPartyEntitlementFormField> entitlements = new ArrayList<>();

    @Valid
    private List<RelyingPartyEaaFormField> eaas = new ArrayList<>();

    public EditRelyingPartyResource toResource() {
        return new EditRelyingPartyResource(
            this.tradeName,
            this.getEntitlements()
                .stream()
                .filter(RelyingPartyEntitlementFormField::isSet)
                .map(RelyingPartyEntitlementFormField::toResource)
                .toList(),
            this.getEaas()
                .stream()
                .filter(RelyingPartyEaaFormField::isSet)
                .map(RelyingPartyEaaFormField::toResource)
                .toList(),
            true
        );
    }
}

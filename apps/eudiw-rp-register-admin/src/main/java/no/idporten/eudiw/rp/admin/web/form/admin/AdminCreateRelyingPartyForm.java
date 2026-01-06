package no.idporten.eudiw.rp.admin.web.form.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;

import no.idporten.validators.orgnr.Orgnr;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@With
@EqualsAndHashCode
public class AdminCreateRelyingPartyForm {

    @Orgnr(message = "Ugyldig organisasjonsnummer")
    @NotNull
    private String orgno = "";

    @SaneStringConstraint(message =
        "Ugyldig tenestenamn. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Tenestenamn må fyllast ut")
    private String tradeName = "";

    @Valid
    @NotEmpty(message = "Brukarstaden må ha minst ein rolle valt")
    private List<RelyingPartyEntitlementFormField> entitlements = new ArrayList<>();
    @Valid
    private List<RelyingPartyEaaFormField> eaas = new ArrayList<>();

    public CreateRelyingPartyResource toResource() {
        return new CreateRelyingPartyResource(
            this.orgno,
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
                .toList()
        );
    }

    public List<String> getEntitlementValues() {
        return entitlements.stream()
                           .map(RelyingPartyEntitlementFormField::getEntitlement)
                           .toList();
    }
}

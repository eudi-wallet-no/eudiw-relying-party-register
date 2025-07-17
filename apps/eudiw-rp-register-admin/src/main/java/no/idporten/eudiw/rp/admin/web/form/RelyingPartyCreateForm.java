package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;

import no.idporten.validators.orgnr.Orgnr;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyCreateForm {

    @Orgnr(message = "Ikkje et gyldig organisasjonsnummer")
    @NotNull
    private String orgno;

    @SaneStringConstraint(message =
        "Navnet får bare inneholde norske bokstaver, tal, mellemrom, og symbolene "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Navnet får ikkje være tomt")
    private String name;

    private boolean publicSector;

    @Valid
    @NotEmpty(message = "Brukerstedet må ha minst en entitlement")
    private List<RelyingPartyEntitlementFormField> entitlements;
    @Valid
    private List<RelyingPartyEaaFormField> eaas;

    @SuppressWarnings("unused") // used in Spring data binding
    public RelyingPartyCreateForm() {
        this("", "", true, new ArrayList<>(), new ArrayList<>());
    }

    public CreateRelyingPartyResource toResource() {
        return new CreateRelyingPartyResource(
            this.orgno,
            this.name,
            this.publicSector,
            this.getEntitlements()
                .stream()
                .map(RelyingPartyEntitlementFormField::toResource)
                .toList(),
            this.getEaas()
                .stream()
                .filter(RelyingPartyEaaFormField::isSet)
                .map(RelyingPartyEaaFormField::toResource)
                .toList()
        );
    }
}

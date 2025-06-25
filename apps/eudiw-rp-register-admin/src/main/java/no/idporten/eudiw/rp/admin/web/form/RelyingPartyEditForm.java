package no.idporten.eudiw.rp.admin.web.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.util.*;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEditForm {
    @SaneStringConstraint(message =
        "Navnet får bare inneholde norske bokstaver, tal, mellemrom, og symbolene "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Navnet får ikkje være tomt")
    private String name;

    private boolean publicSector;

    @Valid
    private List<RelyingPartyEntitlementFormField> entitlements;

    @Valid
    private List<RelyingPartyEaaFormField> eaas;

    private boolean active;

    @SuppressWarnings("unused") // used in Spring data binding
    public RelyingPartyEditForm() {
        this("", true, new ArrayList<>(), new ArrayList<>(), true);
    }

    public EditRelyingPartyResource toResource() {
        return new EditRelyingPartyResource(
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
                .toList(),
            this.active
        );
    }

    public static RelyingPartyEditForm prefillFromRelyingPartyResource(
        RelyingPartyResource resource) {
        return new RelyingPartyEditForm(
            resource.name(),
            resource.publicSector(),
            resource.relyingPartyEntitlements()
                    .stream()
                    .map(RelyingPartyEntitlementFormField::fromResource)
                    .toList(),
            resource.relyingPartyEaas()
                    .stream()
                    .map(RelyingPartyEaaFormField::fromResource)
                    .toList(),
            resource.active()
        );
    }
}

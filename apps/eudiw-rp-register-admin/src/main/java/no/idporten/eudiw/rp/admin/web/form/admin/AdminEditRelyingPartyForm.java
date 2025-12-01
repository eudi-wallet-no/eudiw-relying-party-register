package no.idporten.eudiw.rp.admin.web.form.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEntitlementFormField;
import no.idporten.eudiw.rp.admin.web.resource.EditRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;

import java.util.*;

@AllArgsConstructor
@Getter
@Setter
@With
@EqualsAndHashCode
public class AdminEditRelyingPartyForm {
    @SaneStringConstraint(message =
        "Ugyldig tenestenamn. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Tenestenamn må fyllast ut")
    private String tradeName;

    @Valid
    @NotEmpty(message = "Brukerstedet må ha minst en entitlement")
    private List<RelyingPartyEntitlementFormField> entitlements;

    @Valid
    private List<RelyingPartyEaaFormField> eaas;

    @NotBlank(message = "url-en til .well-known/openid-credential-issuer endepunkt må registreres")
    private String credentialIssuerUrl;

    @Valid
    private boolean active;

    @SuppressWarnings("unused") // used in Spring data binding
    public AdminEditRelyingPartyForm() {
        this("", new ArrayList<>(), new ArrayList<>(), "", true);
    }

    public EditRelyingPartyResource toResource() {
        return new EditRelyingPartyResource(
            this.tradeName,
            this.getEntitlements()
                .stream()
                .map(RelyingPartyEntitlementFormField::toResource)
                .toList(),
            this.getEaas()
                .stream()
                .filter(RelyingPartyEaaFormField::isSet)
                .map(RelyingPartyEaaFormField::toResource)
                .toList(),
            this.credentialIssuerUrl,
            this.active
        );
    }

    public static AdminEditRelyingPartyForm prefillFromRelyingPartyResource(
        RelyingPartyResource resource) {
        return new AdminEditRelyingPartyForm(
            resource.tradeName(),
            resource.relyingPartyEntitlements()
                    .stream()
                    .map(RelyingPartyEntitlementFormField::fromResource)
                    .toList(),
            resource.relyingPartyEaas()
                    .stream()
                    .map(RelyingPartyEaaFormField::fromResource)
                    .toList(),
            resource.credentialIssuerUrl(),
            resource.active()
        );
    }

    public List<String> getEntitlementValues() {
        return entitlements.stream()
                           .map(RelyingPartyEntitlementFormField::getEntitlement)
                           .toList();
    }
}

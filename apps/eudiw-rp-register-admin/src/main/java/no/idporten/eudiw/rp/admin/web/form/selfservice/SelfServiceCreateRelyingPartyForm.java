package no.idporten.eudiw.rp.admin.web.form.selfservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.validation.SaneStringValidator;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEaaFormField;
import no.idporten.eudiw.rp.admin.web.resource.CreateRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SelfServiceCreateRelyingPartyForm {

    @SaneStringConstraint(message =
        "Ugyldig tenestenamn. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Tenestenamn må fyllast ut")
    private String tradeName = "";

    @Valid
    private List<@Valid RelyingPartyEaaFormField> eaas = new ArrayList<>();

    public static final List<RelyingPartyEntitlementResource> DEFAULT_NON_ADMIN_ENTITLEMENTS =
        List.of(new RelyingPartyEntitlementResource(
            "https://uri.etsi.org/19475/Entitlement/Service_Provider"));

    public CreateRelyingPartyResource toResource(String orgno) {
        return new CreateRelyingPartyResource(
            orgno,
            this.tradeName,
            DEFAULT_NON_ADMIN_ENTITLEMENTS,
            this.getEaas()
                .stream()
                .filter(RelyingPartyEaaFormField::isSet)
                .map(RelyingPartyEaaFormField::toResource)
                .toList()
        );
    }
}

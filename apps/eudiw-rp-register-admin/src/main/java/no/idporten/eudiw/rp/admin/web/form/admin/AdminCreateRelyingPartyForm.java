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
        "Ugyldig namn. Gyldige teikn er: norske bokstavar, tal, mellemrom og symbola "
            + SaneStringValidator.ALLOWED_SYMBOLS)
    @NotBlank(message = "Namn må fyllast ut")
    private String name = "";


    //TODO: Dynamisk URL, ikke oppfordring til å legge inn denne. Gjøres når folk begynner å utstede sjolv
    @NotBlank(message= "Utsteder url må legges til. Dette er: https://utsteder.test.eidas2sandkasse.net/.well-known/openid-credential-issuer inntil videre")
    private String credentialIssuerUrl = "";

    private boolean publicSector = true;

    @Valid
    @NotEmpty(message = "Brukarstaden må ha minst ein rolle valt")
    private List<RelyingPartyEntitlementFormField> entitlements = new ArrayList<>();
    @Valid
    private List<RelyingPartyEaaFormField> eaas = new ArrayList<>();

    public CreateRelyingPartyResource toResource() {
        return new CreateRelyingPartyResource(
            this.orgno,
            this.name,
            this.credentialIssuerUrl,
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

    public List<String> getEntitlementValues() {
        return entitlements.stream()
                           .map(RelyingPartyEntitlementFormField::getEntitlement)
                           .toList();
    }
}

package no.idporten.eudiw.rp.admin.web.form.selfservice;

import jakarta.validation.Valid;
import lombok.*;
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
    @Valid
    private List<@Valid RelyingPartyEaaFormField> eaas = new ArrayList<>();

    public static final List<RelyingPartyEntitlementResource> DEFAULT_NON_ADMIN_ENTITLEMENTS =
        List.of(new RelyingPartyEntitlementResource(
            "https://uri.etsi.org/19475/Entitlement/Service_Provider"));

    public CreateRelyingPartyResource toResource(
        String orgno, String name, String credentialIssuerUrl, boolean publicSector) {
        return new CreateRelyingPartyResource(
            orgno,
            name,
            credentialIssuerUrl,
            publicSector,
            DEFAULT_NON_ADMIN_ENTITLEMENTS,
            this.getEaas()
                .stream()
                .filter(RelyingPartyEaaFormField::isSet)
                .map(RelyingPartyEaaFormField::toResource)
                .toList()
        );
    }
}

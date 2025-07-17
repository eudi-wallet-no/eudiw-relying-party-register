package no.idporten.eudiw.rp.admin.web.form;

import lombok.*;
import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;
import no.idporten.eudiw.rp.admin.validation.EntitlementConstraint;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEntitlementFormField {
    @EntitlementConstraint(message = "Den gitte entitlement er ukjent og/eller ugjyldig")
    private String entitlement;

    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(
            RelyingPartyEntitlement.fromString(this.entitlement));
    }
    public static RelyingPartyEntitlementFormField fromResource(
        RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlementFormField(resource.entitlement().getUri());
    }
}

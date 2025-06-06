package no.idporten.eudiw.rp.admin.web.forms;

import lombok.*;
import no.idporten.eudiw.rp.admin.validation.SaneStringConstraint;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEntitlementFormField {
    @SaneStringConstraint
    private String entitlement;

    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(this.getEntitlement());
    }
    public static RelyingPartyEntitlementFormField fromResource(
        RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlementFormField(resource.entitlement());
    }
}

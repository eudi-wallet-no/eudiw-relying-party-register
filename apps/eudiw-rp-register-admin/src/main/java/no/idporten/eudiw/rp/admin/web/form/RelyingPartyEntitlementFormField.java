package no.idporten.eudiw.rp.admin.web.form;

import lombok.*;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEntitlementFormField {
    private String entitlement;

    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(this.entitlement);
    }
    public static RelyingPartyEntitlementFormField fromResource(
        RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlementFormField(resource.entitlement());
    }
}

package no.idporten.eudiw.rp.register.lookup.web.form;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyEntitlementResource;

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

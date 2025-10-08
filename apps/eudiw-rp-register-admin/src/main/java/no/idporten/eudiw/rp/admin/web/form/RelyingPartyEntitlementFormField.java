package no.idporten.eudiw.rp.admin.web.form;

import lombok.*;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

import java.util.ArrayList;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEntitlementFormField {
    private String entitlement;

    //TODO fix
    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(this.entitlement, new ArrayList<>());
    }
    public static RelyingPartyEntitlementFormField fromResource(
        RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlementFormField(resource.entitlement());
    }
}

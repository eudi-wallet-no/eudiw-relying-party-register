package no.idporten.eudiw.rp.admin.web.form;

import lombok.*;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEntitlementFormField {

    private String entitlement;

    private String credentialIssuerUrl;

    public boolean isSet() {
        return this.entitlement != null;
    }

    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(this.entitlement, null, this.credentialIssuerUrl);
    }
    public static RelyingPartyEntitlementFormField fromResource(
        RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlementFormField(resource.entitlement(), resource.credentialIssuerUrl());
    }
}

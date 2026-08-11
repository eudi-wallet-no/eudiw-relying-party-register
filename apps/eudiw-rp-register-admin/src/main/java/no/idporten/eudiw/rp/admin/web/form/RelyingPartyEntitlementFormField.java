package no.idporten.eudiw.rp.admin.web.form;

import lombok.*;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import org.hibernate.validator.constraints.URL;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RelyingPartyEntitlementFormField {

    private String entitlement;

    @URL(protocol = "https", message = "URL'en er ikkje ein gyldig HTTPS-URL")
    private String credentialIssuerUrl;

    @EqualsAndHashCode.Exclude
    private String displayName; // for rendering purposes only!

    public boolean isSet() {
        return this.entitlement != null;
    }

    public RelyingPartyEntitlementResource toResource() {
        return new RelyingPartyEntitlementResource(this.entitlement, null, this.credentialIssuerUrl);
    }
    public static RelyingPartyEntitlementFormField fromResource(
        RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlementFormField(
            resource.entitlement(), resource.credentialIssuerUrl(), resource.displayName());
    }
}

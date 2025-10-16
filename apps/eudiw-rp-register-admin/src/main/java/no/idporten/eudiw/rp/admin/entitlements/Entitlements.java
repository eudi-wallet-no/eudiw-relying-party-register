package no.idporten.eudiw.rp.admin.entitlements;

import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;

import java.util.Objects;

public class Entitlements {
    private Entitlements() { }

    private static final String SERVICE_PROVIDER_URI =
        "https://uri.etsi.org/19475/Entitlement/Service_Provider";

    public static boolean isIssuerEntitlement(String entitlementUri) {
        return !Objects.equals(entitlementUri, SERVICE_PROVIDER_URI);
    }
    public static boolean isIssuerEntitlement(RelyingPartyEntitlementResource rpEntitlementResource) {
        return isIssuerEntitlement(rpEntitlementResource.entitlement());
    }
}

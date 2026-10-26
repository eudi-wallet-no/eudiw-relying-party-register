package no.eudiw.rp.register.api.v2.endpoints.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.eudiw.rp.register.api.v2.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RegistrationDetailsResponse(
    UUID id,
    UUID relyingPartyServiceId,
    UUID relyingPartyId,
    String orgno,
    String organizationName,
    String serviceTradeName,
    boolean isPSB,
    List<RelyingPartyEntitlementResponse> relyingPartyEntitlements,
    List<RelyingPartyEaaResource> relyingPartyEaas,
    List<CertificateResponse> accessCertificates,
    List<CertificateResponse> issuerCertificates,
    long createdMs,
    long lastUpdatedMs,
    boolean active
) {
    public static RegistrationDetailsResponse from(RelyingPartyInstance relyingPartyInstance,
                                                   Map<String, Entitlement> entitlementCatalog) {
        WalletRelyingPartyService relyingPartyService = relyingPartyInstance.getWalletRelyingPartyService();
        WalletRelyingParty relyingParty = relyingPartyService.getWalletRelyingParty();
        return new RegistrationDetailsResponse(
            relyingPartyInstance.getId(), relyingPartyService.getId(), relyingParty.getId(),
            relyingParty.getOrgno(), relyingParty.getName(), relyingPartyService.getServiceTradeName(), relyingParty.isPSB(),
            relyingPartyInstance.getRelyingPartyEntitlements().stream()
                .map(relyingPartyEntitlement -> new RelyingPartyEntitlementResponse(
                    relyingPartyEntitlement.getEntitlement(),
                    displayName(relyingPartyEntitlement.getEntitlement(), entitlementCatalog),
                    relyingPartyEntitlement.getCredentialIssuerUrl())).toList(),
            relyingPartyInstance.getRelyingPartyEaas().stream()
                .map(relyingPartyEaa -> new RelyingPartyEaaResource(relyingPartyEaa.getNamespace(), relyingPartyEaa.getIntent())).toList(),
            relyingPartyInstance.getAccessCertificates().stream().map(CertificateResponse::from).toList(),
            relyingPartyInstance.getIssuerCertificates().stream().map(CertificateResponse::from).toList(),
            relyingPartyInstance.getCreatedMs(), relyingPartyInstance.getLastUpdatedMs(), relyingPartyInstance.isActive());
    }

    private static String displayName(String entitlementUri, Map<String, Entitlement> entitlementCatalog) {
        Entitlement entitlement = entitlementCatalog.get(entitlementUri);
        return entitlement == null ? entitlementUri : entitlement.getDisplayName();
    }

    public record RelyingPartyEntitlementResponse(String entitlement, String displayName, String credentialIssuerUrl) {
    }
}

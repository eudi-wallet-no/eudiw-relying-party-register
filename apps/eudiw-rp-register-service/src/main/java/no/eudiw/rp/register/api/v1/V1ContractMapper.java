package no.eudiw.rp.register.api.v1;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.v1.resource.entitlements.EntitlementResource;
import no.eudiw.rp.register.api.v1.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyResource;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.EntitlementRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class V1ContractMapper {

    private final EntitlementRepository entitlementRepository;

    public RelyingPartyResource toV1RelyingPartyResponse(RelyingPartyInstance instance) {
        WalletRelyingPartyService walletService = instance.getWalletRelyingPartyService();
        WalletRelyingParty walletRelyingParty = walletService.getWalletRelyingParty();

        return new RelyingPartyResource(
            instance.getId(),
            walletRelyingParty.getOrgno(),
            walletRelyingParty.getName(),
            walletService.getServiceTradeName(),
            walletRelyingParty.isPSB(),
            instance.getRelyingPartyEntitlements().stream()
                .map(this::toV1RelyingPartyEntitlementResponse).toList(),
            instance.getRelyingPartyEaas().stream().map(this::toV1RelyingPartyEaaResponse).toList(),
            instance.getAccessCertificates().stream().map(this::toV1CertificateResponse).toList(),
            instance.getIssuerCertificates().stream().map(this::toV1CertificateResponse).toList(),
            instance.getCreatedMs(),
            instance.getLastUpdatedMs(),
            instance.isActive()
        );
    }

    public RelyingPartyEntitlementResource toV1RelyingPartyEntitlementResponse(
        RelyingPartyEntitlement entitlement) {
        return new RelyingPartyEntitlementResource(
            entitlement.getEntitlement(),
            getDisplayNameForEntitlement(entitlement.getEntitlement()),
            entitlement.getCredentialIssuerUrl()
        );
    }

    private String getDisplayNameForEntitlement(String entitlementUri) {
        return entitlementRepository.findByEntitlement(entitlementUri)
            .map(Entitlement::getDisplayName)
            .orElse(entitlementUri);
    }

    public RelyingPartyCertificateResource toV1CertificateResponse(AccessCertificate certificate) {
        return new RelyingPartyCertificateResource(
            certificate.getCertificate(), null, certificate.getId(), certificate.getRevocationStatus());
    }

    public RelyingPartyCertificateResource toV1CertificateResponse(IssuerCertificate certificate) {
        return new RelyingPartyCertificateResource(
            certificate.getCertificate(),
            certificate.getEntitlement().getEntitlement(),
            certificate.getId(),
            certificate.getRevocationStatus()
        );
    }

    public RelyingPartyEaaResource toV1RelyingPartyEaaResponse(RelyingPartyEaa eaa) {
        return new RelyingPartyEaaResource(eaa.getNamespace(), eaa.getIntent());
    }

    public EntitlementResource toV1EntitlementResponse(Entitlement entitlement) {
        return new EntitlementResource(
            entitlement.getId(),
            entitlement.getEntitlement(),
            entitlement.isActive(),
            entitlement.getDisplayName(),
            entitlement.getCaId()
        );
    }

    public EntitlementsResource toV1EntitlementsResponse(List<Entitlement> entitlements) {
        return new EntitlementsResource(entitlements.stream().map(this::toV1EntitlementResponse).toList());
    }

    public List<RelyingPartyEntitlement> toDomainRelyingPartyEntitlements(
        List<RelyingPartyEntitlementResource> v1EntitlementResources) {
        if (v1EntitlementResources == null) {
            return null;
        }
        return v1EntitlementResources.stream()
            .map(v1Entitlement -> new RelyingPartyEntitlement(
                v1Entitlement.entitlement(), v1Entitlement.credentialIssuerUrl()))
            .toList();
    }

    public List<RelyingPartyEaa> toDomainRelyingPartyEaas(List<RelyingPartyEaaResource> v1EaaResources) {
        if (v1EaaResources == null) {
            return null;
        }
        return v1EaaResources.stream()
            .map(v1Eaa -> new RelyingPartyEaa(v1Eaa.namespace(), v1Eaa.intent()))
            .toList();
    }
}

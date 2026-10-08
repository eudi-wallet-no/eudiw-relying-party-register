package no.eudiw.rp.register.api.v1;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.v1.resource.entitlements.EntitlementResource;
import no.eudiw.rp.register.api.v1.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyResource;
import no.eudiw.rp.register.domain.Entitlement;
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

    public RelyingPartyResource toV1RelyingPartyResource(RelyingPartyInstance relyingPartyInstance) {
        return new RelyingPartyResource(
            relyingPartyInstance.getId(),
            relyingPartyInstance.getWalletRelyingPartyService().getWalletRelyingParty().getOrgno(),
            relyingPartyInstance.getWalletRelyingPartyService().getWalletRelyingParty().getLegalName(),
            relyingPartyInstance.getWalletRelyingPartyService().getServiceTradeName(),
            relyingPartyInstance.getWalletRelyingPartyService().getWalletRelyingParty().isPsb(),
            relyingPartyInstance.getRelyingPartyEntitlements().stream()
                .map(this::toV1RelyingPartyEntitlementResource).toList(),
            relyingPartyInstance.getRelyingPartyEaas().stream().map(this::toV1RelyingPartyEaaResource).toList(),
            relyingPartyInstance.getAccessCertificates().stream().map(this::toV1CertificateResource).toList(),
            relyingPartyInstance.getIssuerCertificates().stream().map(this::toV1CertificateResource).toList(),
            relyingPartyInstance.getCreatedMs(),
            relyingPartyInstance.getLastUpdatedMs(),
            relyingPartyInstance.isActive()
        );
    }

    public RelyingPartyEntitlementResource toV1RelyingPartyEntitlementResource(
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

    public RelyingPartyCertificateResource toV1CertificateResource(AccessCertificate entity) {
        return new RelyingPartyCertificateResource(
            entity.getCertificate(), null, entity.getId(), entity.getRevocationStatus());
    }

    public RelyingPartyCertificateResource toV1CertificateResource(IssuerCertificate entity) {
        return new RelyingPartyCertificateResource(
            entity.getCertificate(),
            entity.getEntitlement().getEntitlement(),
            entity.getId(),
            entity.getRevocationStatus()
        );
    }

    public RelyingPartyEaaResource toV1RelyingPartyEaaResource(RelyingPartyEaa eaa) {
        return new RelyingPartyEaaResource(eaa.getNamespace(), eaa.getIntent());
    }

    public EntitlementResource toV1EntitlementResource(Entitlement entitlement) {
        return new EntitlementResource(
            entitlement.getId(),
            entitlement.getEntitlement(),
            entitlement.isActive(),
            entitlement.getDisplayName(),
            entitlement.getCaId()
        );
    }

    public EntitlementsResource toV1EntitlementsResource(List<Entitlement> entitlements) {
        return new EntitlementsResource(entitlements.stream().map(this::toV1EntitlementResource).toList());
    }

    public List<RelyingPartyEntitlement> toDomainRelyingPartyEntitlements(
        List<RelyingPartyEntitlementResource> resources) {
        if (resources == null) {
            return null;
        }
        return resources.stream()
            .map(resource -> new RelyingPartyEntitlement(
                resource.entitlement(), resource.credentialIssuerUrl()))
            .toList();
    }

    public List<RelyingPartyEaa> toDomainRelyingPartyEaas(List<RelyingPartyEaaResource> resources) {
        if (resources == null) {
            return null;
        }
        return resources.stream()
            .map(resource -> new RelyingPartyEaa(resource.namespace(), resource.intent()))
            .toList();
    }
}

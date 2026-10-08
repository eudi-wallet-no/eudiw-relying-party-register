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
public class V1DataConverter {

    private final EntitlementRepository entitlementRepository;

    public RelyingPartyResource toResource(RelyingPartyInstance relyingPartyInstance) {
        return new RelyingPartyResource(
            relyingPartyInstance.getId(),
            relyingPartyInstance.getLegalEntity().getOrgno(),
            relyingPartyInstance.getLegalEntity().getName(),
            relyingPartyInstance.getTradeName(),
            relyingPartyInstance.getLegalEntity().isPublicSector(),
            relyingPartyInstance.getRelyingPartyEntitlements().stream().map(this::toResource).toList(),
            relyingPartyInstance.getRelyingPartyEaas().stream().map(this::toResource).toList(),
            relyingPartyInstance.getAccessCertificates().stream().map(this::toResource).toList(),
            relyingPartyInstance.getIssuerCertificates().stream().map(this::toResource).toList(),
            relyingPartyInstance.getCreatedMs(),
            relyingPartyInstance.getLastUpdatedMs(),
            relyingPartyInstance.isActive()
        );
    }

    public RelyingPartyEntitlementResource toResource(RelyingPartyEntitlement entitlement) {
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

    public RelyingPartyCertificateResource toResource(AccessCertificate entity) {
        return new RelyingPartyCertificateResource(
            entity.getCertificate(), null, entity.getId(), entity.getRevocationStatus());
    }

    public RelyingPartyCertificateResource toResource(IssuerCertificate entity) {
        return new RelyingPartyCertificateResource(
            entity.getCertificate(),
            entity.getEntitlement().getEntitlement(),
            entity.getId(),
            entity.getRevocationStatus()
        );
    }

    public RelyingPartyEaaResource toResource(RelyingPartyEaa eaa) {
        return new RelyingPartyEaaResource(eaa.getNamespace(), eaa.getIntent());
    }

    public EntitlementResource toResource(Entitlement entitlement) {
        return new EntitlementResource(
            entitlement.getId(),
            entitlement.getEntitlement(),
            entitlement.isActive(),
            entitlement.getDisplayName(),
            entitlement.getCaId()
        );
    }

    public EntitlementsResource toEntitlementsResource(List<Entitlement> entitlements) {
        return new EntitlementsResource(entitlements.stream().map(this::toResource).toList());
    }

    public List<RelyingPartyEntitlement> toEntitlements(List<RelyingPartyEntitlementResource> resources) {
        if (resources == null) {
            return null;
        }
        return resources.stream()
            .map(resource -> new RelyingPartyEntitlement(
                resource.entitlement(), resource.credentialIssuerUrl()))
            .toList();
    }

    public List<RelyingPartyEaa> toEaas(List<RelyingPartyEaaResource> resources) {
        if (resources == null) {
            return null;
        }
        return resources.stream()
            .map(resource -> new RelyingPartyEaa(resource.namespace(), resource.intent()))
            .toList();
    }
}

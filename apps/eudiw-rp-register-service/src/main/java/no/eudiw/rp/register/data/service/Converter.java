package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.data.entity.*;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Converter {

    private final EntitlementRepository entitlementRepository;

    public RelyingPartyResource toResource(RelyingPartyInstance relyingPartyInstance) {
        return new RelyingPartyResource(
            relyingPartyInstance.getId(),
            relyingPartyInstance.getLegalEntity().getOrgno(),
            relyingPartyInstance.getLegalEntity().getName(),
            relyingPartyInstance.getTradeName(),
            relyingPartyInstance.getLegalEntity().isPublicSector(),
            relyingPartyInstance.getRelyingPartyEntitlements()
                .stream()
                .map(this::toResource)
                .toList(),
            relyingPartyInstance.getRelyingPartyEaas()
                .stream()
                .map(this::toResource)
                .toList(),
            relyingPartyInstance.getCreatedMs(),
            relyingPartyInstance.getLastUpdatedMs(),
            relyingPartyInstance.isActive()
        );
    }

    public List<RelyingPartyResource> toResource(LegalEntity legalEntity) {
        return legalEntity.getRelyingPartyInstances().stream().map(this::toResource).toList();
    }

    public List<RelyingPartyResource> toResource(List<LegalEntity> legalEntities) {
        return legalEntities.stream()
            .flatMap(le -> toResource(le).stream())
            .toList();
    }

    public RelyingPartyEntitlementResource toResource(RelyingPartyEntitlement entitlement) {
        return new RelyingPartyEntitlementResource(
            entitlement.getEntitlement(),
            getDisplayNameForEntitlement(entitlement.getEntitlement()),
            entitlement.getIssuerCertificates().stream().map(this::toResource).toList()
        );
    }

    private String getDisplayNameForEntitlement(String entitlementUri) {
        return entitlementRepository.findByEntitlement(entitlementUri)
            .map(Entitlement::getDisplayName)
            .orElse(entitlementUri);
    }

    public RelyingPartyCertificateResource toResource(BaseCertificateEntity entity) {
        return new RelyingPartyCertificateResource(entity.getCertificate(), entity.getId());
    }

    public RelyingPartyEaaResource toResource(RelyingPartyEaa eaa) {
        return new RelyingPartyEaaResource(eaa.getNamespace(), eaa.getIntent());
    }

    public EntitlementResource toResource(Entitlement entitlement) {
        return new EntitlementResource(entitlement.getId(), entitlement.getEntitlement(), entitlement.isActive(), entitlement.getDisplayName(), entitlement.getCaId());
    }

    public EntitlementsResource toEntitlementsResource(List<Entitlement> entitlements) {
        return new EntitlementsResource(
            entitlements.stream().map(this::toResource).toList()
        );
    }

    public RelyingPartyInstance toEntity(CreateRelyingPartyResource resource) {
        return new RelyingPartyInstance(
            resource.name(),
            resource.relyingPartyEntitlements().stream().map(this::toEntity).toList(),
            resource.relyingPartyEaas().stream().map(this::toEntity).toList(),
            new ArrayList<>()
        );
    }

    public RelyingPartyEntitlement toEntity(RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlement(resource.entitlement());
    }
    public RelyingPartyEaa toEntity(RelyingPartyEaaResource resource) {
        return new RelyingPartyEaa(resource.namespace(), resource.intent());
    }
}

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

    public RelyingPartyResource toResource(RelyingParty relyingParty) {
        return new RelyingPartyResource(
            relyingParty.getId(),
            relyingParty.getOrgno(),
            relyingParty.getName(),
            relyingParty.isPublicSector(),
            relyingParty.getRelyingPartyEntitlements()
                        .stream()
                        .map(this::toResource)
                        .toList(),
            relyingParty.getRelyingPartyEaas()
                        .stream()
                        .map(this::toResource)
                        .toList(),
            relyingParty.getCreatedMs(),
            relyingParty.getLastUpdatedMs(),
            relyingParty.isActive()
        );
    }

    public RelyingPartyEaaResource toResource(RelyingPartyEaa eaa) {
        return new RelyingPartyEaaResource(eaa.getNamespace(), eaa.getIntent());
    }

    private String getDisplayNameForEntitlement(String entitlementUri) {
        return entitlementRepository.findByEntitlement(entitlementUri)
                                    .map(Entitlement::getDisplayName)
                                    .orElse(entitlementUri);
    }
    public RelyingPartyEntitlementResource toResource(RelyingPartyEntitlement entitlement) {
        return new RelyingPartyEntitlementResource(
            entitlement.getEntitlement(),
            getDisplayNameForEntitlement(entitlement.getEntitlement()),
            entitlement.getIssuerCertificates().stream().map(this::toResource).toList()
        );
    }

    public EntitlementResource toResource(Entitlement entitlement) {
        return new EntitlementResource(entitlement.getId(), entitlement.getEntitlement(), entitlement.isActive(), entitlement.getDisplayName(), entitlement.getCaId());
    }

    public EntitlementsResource toEntitlementsResource(List<Entitlement> entitlements) {
        return new EntitlementsResource(
            entitlements.stream().map(this::toResource).toList()
        );
    }

    public RelyingParty toEntity(CreateRelyingPartyResource resource) {
        return new RelyingParty(
            resource.name(),
            resource.orgNr(),
            resource.publicSector(),
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

    public RelyingPartyCertificateResource toResource(BaseCertificateEntity entity) {
        return new RelyingPartyCertificateResource(entity.getCertificate(), entity.getId());
    }
}

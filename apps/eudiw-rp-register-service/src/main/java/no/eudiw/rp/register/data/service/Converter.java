package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.data.entity.*;

import java.util.ArrayList;
import java.util.List;

public class Converter {

    public static RelyingPartiesResource toResource(List<RelyingParty> relyingParties) {
        return new RelyingPartiesResource(
            relyingParties.stream().map(Converter::toResource).toList()
        );
    }

    public static RelyingPartyResource toResource(RelyingParty relyingParty) {
        return new RelyingPartyResource(
            relyingParty.getId(),
            relyingParty.getOrgno(),
            relyingParty.getName(),
            relyingParty.getPublicSector(),
            relyingParty.getRelyingPartyEntitlements()
                        .stream()
                        .map(Converter::toResource)
                        .toList(),
            relyingParty.getRelyingPartyEaas()
                        .stream()
                        .map(Converter::toResource)
                        .toList(),
            relyingParty.getCreatedMs(),
            relyingParty.getLastUpdatedMs(),
            relyingParty.isActive()
        );
    }

    public static RelyingPartyEaaResource toResource(RelyingPartyEaa eaa) {
        return new RelyingPartyEaaResource(eaa.getNamespace(), eaa.getIntent());
    }
    public static RelyingPartyEntitlementResource toResource(RelyingPartyEntitlement entitlement) {
        return new RelyingPartyEntitlementResource(entitlement.getEntitlement());
    }

    public static EntitlementResource toResource(Entitlement entitlement) {
        return new EntitlementResource(entitlement.getId(), entitlement.getEntitlement(), entitlement.isActive());
    }

    public static EntitlementsResource toEntitlementsResource(List<Entitlement> entitlements) {
        return new EntitlementsResource(
            entitlements.stream().map(Converter::toResource).toList()
        );
    }

    public static RelyingParty toEntity(CreateRelyingPartyResource resource) {
        return new RelyingParty(
            resource.name(),
            resource.orgNr(),
            resource.publicSector(),
            resource.relyingPartyEntitlements().stream().map(Converter::toEntity).toList(),
            resource.relyingPartyEaas().stream().map(Converter::toEntity).toList(),
            new ArrayList<>()
        );
    }

    public static Entitlement toEntity(String entitlement) {
        return new Entitlement(
            entitlement,
            true
        );
    }

    public static RelyingPartyEntitlement toEntity(RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlement(resource.entitlement());
    }
    public static RelyingPartyEaa toEntity(RelyingPartyEaaResource resource) {
        return new RelyingPartyEaa(resource.namespace(), resource.intent());
    }

    public static RelyingPartyCertificate toEntity(RelyingPartyCertificateResource resource) {
        return new RelyingPartyCertificate(resource.certificate());
    }
    public static RelyingPartyCertificateResource toResource(RelyingPartyCertificate entity) {
        return new RelyingPartyCertificateResource(entity.getCertificate(), entity.getId());
    }

}

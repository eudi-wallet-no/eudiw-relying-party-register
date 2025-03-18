package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;

import java.util.List;

class Converter {

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

    public static RelyingParty toEntity(CreateRelyingPartyResource resource) {
        return new RelyingParty(
            resource.getName(),
            resource.getOrgNr(),
            resource.isPublicSector(),
            resource.getRelyingPartyEntitlements().stream().map(Converter::toEntity).toList(),
            resource.getRelyingPartyEaas().stream().map(Converter::toEntity).toList()
        );
    }

    public static RelyingPartyEntitlement toEntity(RelyingPartyEntitlementResource resource) {
        return new RelyingPartyEntitlement(resource.getEntitlement());
    }
    public static RelyingPartyEaa toEntity(RelyingPartyEaaResource resource) {
        return new RelyingPartyEaa(resource.getNamespace(), resource.getIntent());
    }

}

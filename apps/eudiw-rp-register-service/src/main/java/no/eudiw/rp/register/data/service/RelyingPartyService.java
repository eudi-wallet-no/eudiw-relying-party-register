package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.*;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RelyingPartyService {

    private final RelyingPartyRepository relyingPartyRepository;

    @Transactional
    public RelyingPartyResource createRelyingParty(CreateRelyingPartyResource request) {
        // TODO: Jira EUW-27 (https://digdir.atlassian.net/browse/EUW-27)
        if (relyingPartyRepository.existsByOrgno(request.getOrgNr())) {
            throw new RelyingPartyRegisterServiceException(
                "Orgno already exists in relyingPartyRepository");
        }

        if (request.getRelyingPartyEntitlements() == null || request.getRelyingPartyEaas() == null) {
            throw new RelyingPartyRegisterServiceException(
                "RelyingPartyEntitlements and RelyingPartyEaas must be non-null");
        }

        RelyingParty relyingParty = Converter.toEntity(request);
        return Converter.toResource(relyingPartyRepository.save(relyingParty));
    }

    @Transactional(readOnly = true)
    public RelyingPartyResource findRelyingParty(UUID id) {
        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElse(null);
        if (relyingParty == null) {
            throw new ApiException("not_found", "Relying Party not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        return Converter.toResource(relyingParty);
    }

    @Transactional(readOnly = true)
    public RelyingPartiesResource searchRelyingParties(SearchRelyingPartyResource request) {
        return new RelyingPartiesResource(
            relyingPartyRepository.findByOrgnoAndOptionalPublicSector(
                                      request.orgno(),
                                      request.publicSector(),
                                      request.includeInactive()
                                  )
                                  .stream()
                                  .map(Converter::toResource)
                                  .toList()
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartiesResource findAllRelyingParties() {
        return Converter.toResource(relyingPartyRepository.findAllByDeleted(false));
    }

    @Transactional
    public RelyingPartyResource updateRelyingParty(UUID id, EditRelyingPartyResource request) {
        // updating requires RelyingParty ID to be set; otherwise it is a creation.
        // TODO: Jira EUW-25 (https://digdir.atlassian.net/browse/EUW-25)
        if (id == null) {
            throw new RelyingPartyRegisterServiceException(
                    "Cannot query update without RelyingParty ID");
        }

        if (request.getRelyingPartyEntitlements() == null || request.getRelyingPartyEaas() == null) {
            throw new RelyingPartyRegisterServiceException(
                    "RelyingPartyEntitlements og RelyingPartyEaas kan ikke være null");
        }

        if (!relyingPartyRepository.existsById(id)) {
            throw new ApiException("not_found", "Relying Party not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElseThrow();
        relyingParty.setName(request.getName());
        relyingParty.setPublicSector(request.isPublicSector());
        relyingParty.setActive(request.isActive());
        setEntitlementsAndEaa(relyingParty, request.getRelyingPartyEntitlements(), request.getRelyingPartyEaas());

        return Converter.toResource(relyingPartyRepository.save(relyingParty));
    }

    @Transactional
    public void deleteRelyingParty(UUID id) {
        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElse(null);
        if (relyingParty == null) {
            throw new ApiException("not_found", "Relying Party not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        relyingParty.setDeleted(true);
        relyingPartyRepository.save(relyingParty);
    }

    private void setEntitlementsAndEaa(
            RelyingParty relyingParty,
            List<RelyingPartyEntitlementResource> entitlements,
            List<RelyingPartyEaaResource> eaas
    ) {
        Set<String> entitlementNames = entitlements.stream()
                .map(RelyingPartyEntitlementResource::getEntitlement)
                .collect(Collectors.toSet());

        List<RelyingPartyEntitlement> updatedEntitlements = relyingParty.getRelyingPartyEntitlements()
                .stream()
                .filter(entitlement -> entitlementNames.contains(entitlement.getEntitlement()))
                .collect(Collectors.toList());

        Set<String> existingEntitlementNames = relyingParty.getRelyingPartyEntitlements()
                .stream()
                .map(RelyingPartyEntitlement::getEntitlement)
                .collect(Collectors.toSet());

        entitlements.stream()
                .map(RelyingPartyEntitlementResource::getEntitlement)
                .filter(entitlement -> !existingEntitlementNames.contains(entitlement))
                .map(entitlement -> new RelyingPartyEntitlement(entitlement, relyingParty))
                .forEach(updatedEntitlements::add);

        relyingParty.setRelyingPartyEntitlements(updatedEntitlements);

        List<RelyingPartyEaa> updatedEaas = eaas.stream()
                .map(eaa -> new RelyingPartyEaa(eaa.getNamespace(), eaa.getIntent(), relyingParty))
                .toList();

        relyingParty.setRelyingPartyEaas(updatedEaas);

    }
}

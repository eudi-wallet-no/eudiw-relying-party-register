package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.data.service.exception.BadRequestException;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import no.eudiw.rp.register.data.service.exception.ResourceDeletedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RelyingPartyService {

    private final RelyingPartyRepository relyingPartyRepository;

    @Transactional
    public RelyingPartyResource createRelyingParty(CreateRelyingPartyResource request) {
        if (relyingPartyRepository.existsByOrgno(request.orgNr())) {
            throw new BadRequestException("Orgnos must be unique");
        }

        if (request.relyingPartyEntitlements() == null || request.relyingPartyEaas() == null) {
            throw new BadRequestException("Entitlements and EAAs should be empty if none exists");
        }

        RelyingParty relyingParty = Converter.toEntity(request);
        return Converter.toResource(relyingPartyRepository.saveAndFlush(relyingParty));
    }

    @Transactional(readOnly = true)
    public RelyingPartyResource findRelyingParty(UUID id) {
        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElse(null);
        if (relyingParty == null) {
            throw new NotFoundException("Relying party not found");
        }

        if (relyingParty.isDeleted()) {
            throw new ResourceDeletedException("No access to relying party resource");
        }

        return Converter.toResource(relyingParty);
    }

    @Transactional(readOnly = true)
    public RelyingPartiesResource searchRelyingParties(
        SearchRelyingPartyResource searchResource) {

        Predicate<RelyingPartyResource> hasRequiredEntitlements =
            rp -> new HashSet<>(rp.relyingPartyEntitlements())
                      .containsAll(searchResource.requiredEntitlements());

        return new RelyingPartiesResource(
            relyingPartyRepository.searchQuery(
                                      searchResource.searchTerm(),
                                      searchResource.includeInactive()
                                  )
                                  .stream()
                                  .map(Converter::toResource)
                                  .filter(hasRequiredEntitlements)
                                  .toList()
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartiesResource advancedSearchRelyingParties(
        AdvancedSearchRelyingPartyResource request) {
        return new RelyingPartiesResource(
            relyingPartyRepository.advancedSearchQuery(
                                      request.orgno(),
                                      request.name(),
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
            throw new BadRequestException("ID should not be null");
        }

        if (request.relyingPartyEntitlements() == null || request.relyingPartyEaas() == null) {
            throw new BadRequestException("Entitlements and EAAs should be emtpy if none exists");
        }

        if (!relyingPartyRepository.existsById(id)) {
            throw new BadRequestException("Relying party not found");
        }

        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElseThrow();
        relyingParty.setName(request.name());
        relyingParty.setPublicSector(request.publicSector());
        relyingParty.setActive(request.active());
        setEntitlementsAndEaa(relyingParty, request.relyingPartyEntitlements(), request.relyingPartyEaas());

        return Converter.toResource(relyingPartyRepository.saveAndFlush(relyingParty));
    }

    @Transactional
    public void deleteRelyingParty(UUID id) {
        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElse(null);
        if (relyingParty == null) {
            throw new NotFoundException("Relying party not found");
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
                .map(RelyingPartyEntitlementResource::entitlement)
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
                .map(RelyingPartyEntitlementResource::entitlement)
                .filter(entitlement -> !existingEntitlementNames.contains(entitlement))
                .map(entitlement -> new RelyingPartyEntitlement(entitlement, relyingParty))
                .forEach(updatedEntitlements::add);

        relyingParty.setRelyingPartyEntitlements(updatedEntitlements);

        List<RelyingPartyEaa> updatedEaas = eaas.stream()
                .map(eaa -> new RelyingPartyEaa(eaa.namespace(), eaa.intent(), relyingParty))
                .toList();

        Set<RelyingPartyEaa> uniqueEaas = new TreeSet<>(Comparator.comparing(p -> p.getIntent() + p.getNamespace()));
        uniqueEaas.addAll(updatedEaas);

        relyingParty.setRelyingPartyEaas(uniqueEaas.stream().toList());
    }
}

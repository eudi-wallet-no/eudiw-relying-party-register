package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.data.service.exception.BadRequestException;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import no.eudiw.rp.register.data.service.exception.ResourceDeletedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
public class RelyingPartyService {

    private final RelyingPartyRepository relyingPartyRepository;
    private final EntitlementRepository entitlementRepository;

    @Transactional
    public RelyingPartyResource createRelyingParty(CreateRelyingPartyResource request) {

        if (request.relyingPartyEntitlements() == null || request.relyingPartyEaas() == null) {
            throw new BadRequestException("Entitlements and EAAs should be empty if none exists");
        }

        entitlementCheck(request.relyingPartyEntitlements());

        RelyingParty relyingParty = Converter.toEntity(request);
        return Converter.toResource(relyingPartyRepository.saveAndFlush(relyingParty));
    }

    private void entitlementCheck(List<RelyingPartyEntitlementResource> entitlements) {
        for (RelyingPartyEntitlementResource entitlement : entitlements) {
            if (!entitlementRepository.existsByEntitlementAndActive(entitlement.entitlement(), true)) {
                throw new BadRequestException(entitlement.entitlement() + " is not a valid active entitlement");
            }
        }
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
    public PagedModel<RelyingPartyResource> searchRelyingParties(SearchRelyingPartyResource searchResource) {

        PageRequest pageRequest =
            PageRequest.of(searchResource.getPage(),
                           searchResource.getPageSize(),
                           searchResource.getOrdering().toSort());

        List<String> entitlements =
            searchResource.getRequiredEntitlements()
                          .stream()
                          .map(RelyingPartyEntitlementResource::entitlement)
                          .toList();

        Page<RelyingParty> page = relyingPartyRepository.searchQueryPaged(
            searchResource.getSearchTerm(),
            searchResource.isIncludeInactive(),
            entitlements,
            pageRequest
        );

        return new PagedModel<>(page.map(Converter::toResource));
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

        entitlementCheck(request.relyingPartyEntitlements());

        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElse(null);
        if (relyingParty == null) {
            throw new BadRequestException("Relying party not found");
        }

        relyingParty.setName(request.name());
        relyingParty.setPublicSector(request.publicSector());
        relyingParty.setActive(request.active());
        
        relyingParty.getRelyingPartyEaas().clear();
        relyingPartyRepository.saveAndFlush(relyingParty);

        relyingParty.setRelyingPartyEntitlements(
            request.relyingPartyEntitlements().stream().map(Converter::toEntity).toList());
        relyingParty.setRelyingPartyEaas(
            request.relyingPartyEaas().stream().map(Converter::toEntity).toList());

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
}

package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.data.service.exception.BadRequestException;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import no.eudiw.rp.register.data.service.exception.ResourceDeletedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;

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
    public RelyingPartiesResource searchRelyingParties(
        SearchRelyingPartyResource searchResource) {

        Predicate<RelyingPartyResource> hasRequiredEntitlements =
            rp -> new HashSet<>(rp.relyingPartyEntitlements())
                      .containsAll(searchResource.getRequiredEntitlements());

        return new RelyingPartiesResource(
            relyingPartyRepository.searchQuery(
                                      searchResource.getSearchTerm(),
                                      searchResource.isIncludeInactive()
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

        entitlementCheck(request.relyingPartyEntitlements());

        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElse(null);
        if (relyingParty == null) {
            throw new BadRequestException("Relying party not found");
        }

        relyingParty.setName(request.name());
        relyingParty.setPublicSector(request.publicSector());
        relyingParty.setActive(request.active());

        relyingParty.getRelyingPartyEntitlements().clear();
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

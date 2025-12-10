package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.entity.RelyingPartyInstance;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.data.repository.LegalEntityRepository;
import no.eudiw.rp.register.data.service.exception.BadRequestException;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartyService {

    private final LegalEntityRepository legalEntityRepository;

    private final EntitlementRepository entitlementRepository;
    private final Converter converter;
    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository;
    private final LegalEntityService legalEntityService;

    @Transactional
    public RelyingPartyResource createRelyingParty(CreateRelyingPartyResource request) {
        if (request.relyingPartyEntitlements() == null || request.relyingPartyEaas() == null) {
            throw new BadRequestException("Entitlements and EAAs should be empty if none exists");
        }
        entitlementCheck(request.relyingPartyEntitlements());

        LegalEntity legalEntity = legalEntityService.getLegalEntityForOrgno(request.orgNr());
        if (!legalEntity.isActive()) {
            throw new BadRequestException("Legal entity is not active");
        }

        RelyingPartyInstance relyingPartyInstance = converter.toEntity(request);
        relyingPartyInstance.setLegalEntity(legalEntity);

        relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
        return converter.toResource(relyingPartyInstance);
    }

    @Transactional(readOnly = true)
    public RelyingPartyResource findRelyingParty(UUID id) {
        RelyingPartyInstance relyingPartyInstance = relyingPartyInstanceRepository.findById(id).orElse(null);
        if (relyingPartyInstance == null) {
            throw new NotFoundException("Relying party not found");
        }

        return converter.toResource(relyingPartyInstance);
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

        Page<RelyingPartyInstance> searchQueryResult =
            relyingPartyInstanceRepository.searchRelyingPartyInstances(
                searchResource.getSearchTerm(),
                entitlements,
                searchResource.isIncludeInactive(),
                searchResource.isHideSyntheticOrgnos(),
                pageRequest
            );

        return new PagedModel<>(searchQueryResult.map(converter::toResource));
    }

    @Transactional
    public RelyingPartyResource updateRelyingParty(UUID id, EditRelyingPartyResource request) {
        if (id == null) {
            throw new BadRequestException("ID should not be null");
        }

        if (request.relyingPartyEntitlements() == null || request.relyingPartyEaas() == null) {
            throw new BadRequestException("Entitlements and EAAs should be emtpy if none exists");
        }

        entitlementCheck(request.relyingPartyEntitlements());

        RelyingPartyInstance relyingPartyInstance = relyingPartyInstanceRepository.findById(id).orElse(null);

        if (relyingPartyInstance == null) {
            throw new BadRequestException("Relying party instance not found");
        }

        relyingPartyInstance.setTradeName(request.tradeName());
        relyingPartyInstance.setActive(request.active());

        relyingPartyInstance.getRelyingPartyEaas().clear();

        relyingPartyInstance.setRelyingPartyEntitlements(
            request.relyingPartyEntitlements().stream().map(converter::toEntity).toList());
        relyingPartyInstance.setRelyingPartyEaas(
            request.relyingPartyEaas().stream().map(converter::toEntity).toList());

        RelyingPartyInstance returnInstance = relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);

        return converter.toResource(returnInstance);
    }

    @Transactional
    public void deleteRelyingParty(UUID id) {
        LegalEntity legalEntity = legalEntityRepository.findById(id).orElse(null);
        if (legalEntity == null) {
            throw new NotFoundException("Relying party not found");
        }
        legalEntityRepository.delete(legalEntity);
    }

    private void entitlementCheck(List<RelyingPartyEntitlementResource> entitlements) {
        for (RelyingPartyEntitlementResource entitlement : entitlements) {
            if (!entitlementRepository.existsByEntitlementAndActive(entitlement.entitlement(), true)) {
                throw new BadRequestException(entitlement.entitlement() + " is not a valid active entitlement");
            }
        }
    }
}

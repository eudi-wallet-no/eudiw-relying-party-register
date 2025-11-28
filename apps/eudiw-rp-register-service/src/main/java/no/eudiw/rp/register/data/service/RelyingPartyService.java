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

import java.util.ArrayList;
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

    @Transactional
    public RelyingPartyResource createRelyingParty(CreateRelyingPartyResource request) {
        if (request.relyingPartyEntitlements() == null || request.relyingPartyEaas() == null) {
            throw new BadRequestException("Entitlements and EAAs should be empty if none exists");
        }
        entitlementCheck(request.relyingPartyEntitlements());

        LegalEntity legalEntity = legalEntityRepository.findByOrgno(request.orgNr()).orElse(
            new LegalEntity(request.name(), request.orgNr(), request.publicSector(), new ArrayList<>())
        );
        if (!legalEntity.isActive()) {
            throw new BadRequestException("Legal entity is not active");
        }

        LegalEntity registeredLegalEntity = legalEntityRepository.saveAndFlush(legalEntity);
        RelyingPartyInstance relyingPartyInstance = converter.toEntity(request);
        registeredLegalEntity.addRelyingPartyInstance(relyingPartyInstance);
        RelyingPartyInstance savedInstance = relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
        return converter.toResource(registeredLegalEntity, savedInstance);
    }

    @Transactional(readOnly = true)
    public RelyingPartyResource findRelyingParty(UUID id) {
        RelyingPartyInstance relyingPartyInstance = relyingPartyInstanceRepository.findById(id).orElse(null);
        if (relyingPartyInstance == null) {
            throw new NotFoundException("Relying party not found");
        }

        return converter.toResource(relyingPartyInstance.getLegalEntity(), relyingPartyInstance);
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

        Page<LegalEntity> page = legalEntityRepository.searchRelyingParties(
            searchResource.getSearchTerm(),
            entitlements,
            new HashSet<>(entitlements).size(),
            searchResource.isIncludeInactive(),
            pageRequest
        );

        List<RelyingPartyResource> resources = converter.toResource(page.getContent());
        Page<RelyingPartyResource> resourcePage = new PageImpl<>(resources, page.getPageable(), page.getTotalElements());
        return new PagedModel<>(resourcePage);
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

        LegalEntity legalEntity = relyingPartyInstance.getLegalEntity();
        if (legalEntity == null) {
            throw new BadRequestException("Relying party not found");
        }

        relyingPartyInstance.setTradeName(request.name());
        relyingPartyInstance.setActive(request.active());
        legalEntity.setName(request.name());
        legalEntity.setActive(request.active());
        legalEntity.setPublicSector(request.publicSector());

        relyingPartyInstance.getRelyingPartyEaas().clear();

        relyingPartyInstance.setRelyingPartyEntitlements(
            request.relyingPartyEntitlements().stream().map(converter::toEntity).toList());
        relyingPartyInstance.setRelyingPartyEaas(
            request.relyingPartyEaas().stream().map(converter::toEntity).toList());

        RelyingPartyInstance returnInstance = relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
        LegalEntity returnRp = legalEntityRepository.saveAndFlush(legalEntity);

        return converter.toResource(returnRp, returnInstance);
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

package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.entitlements.CreateEntitlementResource;
import no.eudiw.rp.register.api.resource.entitlements.EntitlementResource;
import no.eudiw.rp.register.api.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.data.entity.Entitlement;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.service.exception.AlreadyExistsException;
import no.eudiw.rp.register.service.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class EntitlementService {

    private final EntitlementRepository entitlementRepository;
    private final Converter converter;

    @Transactional
    public EntitlementResource register(CreateEntitlementResource entitlement) {
        if (entitlement == null || entitlement.entitlement() == null || entitlement.entitlement().isEmpty()) {
            throw new BadRequestException("Invalid entitlement");
        }

        if (entitlementRepository.existsByEntitlement(entitlement.entitlement())) {
            throw new AlreadyExistsException(
                "Entitlement already exists ");
        }

        Entitlement entity = new Entitlement(entitlement.entitlement(), true, entitlement.displayName(), entitlement.caId());
        return converter.toResource(entitlementRepository.saveAndFlush(entity));
    }

    @Transactional(readOnly = true)
    public EntitlementsResource findAllActive() {
        return converter.toEntitlementsResource(entitlementRepository.findAllByActive(true));
    }

    @Transactional(readOnly = true)
    public EntitlementsResource findAllEntitlements() {
        return converter.toEntitlementsResource(entitlementRepository.findAll());
    }

    @Transactional
    public EntitlementResource editEntitlement(String entitlement, boolean status) {
        if (entitlement == null || entitlement.isEmpty()) {
            throw new BadRequestException("entitlement should not be null");
        }

        Entitlement entity = entitlementRepository.findByEntitlement(entitlement)
                .orElseThrow(() -> new BadRequestException("Entitlement not found"));

        entity.setActive(status);
        return converter.toResource(entitlementRepository.saveAndFlush(entity));
    }
}

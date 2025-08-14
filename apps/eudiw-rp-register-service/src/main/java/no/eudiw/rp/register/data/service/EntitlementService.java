package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.Entitlement;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.data.service.exception.AlreadyExistsException;
import no.eudiw.rp.register.data.service.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class EntitlementService {

    private final EntitlementRepository entitlementRepository;

    @Transactional
    public EntitlementResource register(CreateEntitlementResource entitlement) {
        if (entitlement == null || entitlement.entitlement() == null || entitlement.entitlement().isEmpty()) {
            throw new BadRequestException("Invalid entitlement");
        }

        if (entitlementRepository.existsByEntitlement(entitlement.entitlement())) {
            throw new AlreadyExistsException(
                "Entitlement already exists ");
        }

        Entitlement entity = new Entitlement(entitlement.entitlement(), true);
        return Converter.toResource(entitlementRepository.saveAndFlush(entity));
    }

    @Transactional(readOnly = true)
    public EntitlementsResource findAllActive() {
        return Converter.toEntitlementsResource(entitlementRepository.findAllByActive(true));
    }

    @Transactional(readOnly = true)
    public EntitlementsResource findAllEntitlements() {
        return Converter.toEntitlementsResource(entitlementRepository.findAll());
    }

    @Transactional
    public EntitlementResource editEntitlement(String entitlement, boolean status) {
        if (entitlement == null || entitlement.isEmpty()) {
            throw new BadRequestException("entitlement should not be null");
        }

        Entitlement entity = entitlementRepository.findByEntitlement(entitlement);
        if (entity == null) {
            throw new BadRequestException("Entitlement not found");
        }

        entity.setActive(status);
        return Converter.toResource(entitlementRepository.saveAndFlush(entity));
    }
}

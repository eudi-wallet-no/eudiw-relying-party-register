package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.entitlements.EntitlementResource;
import no.eudiw.rp.register.api.resource.entitlements.EntitlementsResource;
import no.eudiw.rp.register.data.entity.Entitlement;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.service.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class EntitlementService {

    private final EntitlementRepository entitlementRepository;
    private final Converter converter;

    @Transactional(readOnly = true)
    public EntitlementsResource findAllEntitlements(boolean includeInactive) {
        List<Entitlement> entitlements =
            includeInactive
                ? entitlementRepository.findAll()
                : entitlementRepository.findAllByActive(true);
        return converter.toEntitlementsResource(entitlements);
    }

    @Transactional(readOnly = true)
    public Entitlement findByEntitlementUri(String entitlementUri) {
        return this.entitlementRepository
                   .findByEntitlement(entitlementUri)
                   .orElseThrow(() -> new NotFoundException("Entitlement does not exist"));
    }

    @Transactional(readOnly = true)
    public String getDefaultCaForEntitlementUri(String entitlementUri) {
        return this.findByEntitlementUri(entitlementUri).getCaId();
    }
}

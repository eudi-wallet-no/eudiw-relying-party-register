package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class EntitlementService {

    private final EntitlementRepository entitlementRepository;

    @Transactional(readOnly = true)
    public List<Entitlement> findAllEntitlements(boolean includeInactive) {
        return includeInactive
            ? entitlementRepository.findAll()
            : entitlementRepository.findAllByActive(true);
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

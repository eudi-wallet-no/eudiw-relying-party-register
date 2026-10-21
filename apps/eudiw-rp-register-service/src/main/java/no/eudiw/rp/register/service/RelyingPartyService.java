package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RelyingPartyService {

    private final EntitlementRepository entitlementRepository;
    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository;
    private final WalletRelyingPartyRepository walletRelyingPartyRepository;
    private final WalletRelyingPartyLookupService walletRelyingPartyLookupService;
    private final WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository;

    public RelyingPartyInstance createRelyingParty(
        String orgno,
        String serviceTradeName,
        List<RelyingPartyEntitlement> entitlements,
        List<RelyingPartyEaa> eaas
    ) {
        if (entitlements == null || eaas == null) {
            throw new BadRequestException("Entitlements and EAAs should be empty if none exists");
        }
        entitlementCheck(entitlements);

        WalletRelyingParty walletRelyingParty =
            walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(orgno);
        if (!walletRelyingParty.isActive()) {
            throw new BadRequestException("Legal entity is not active");
        }

        RelyingPartyInstance relyingPartyInstance =
            new RelyingPartyInstance(entitlements, eaas, new ArrayList<>());
        WalletRelyingPartyService walletService =
            new WalletRelyingPartyService(serviceTradeName, walletRelyingParty, List.of(relyingPartyInstance));
        walletRelyingParty.getServices().add(walletService);
        walletRelyingPartyServiceRepository.save(walletService);

        return relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
    }

    @Transactional(readOnly = true)
    public RelyingPartyInstance findRelyingParty(UUID id) {
        return relyingPartyInstanceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Relying party not found"));
    }

    @Transactional(readOnly = true)
    public Page<RelyingPartyInstance> searchRelyingParties(
        String searchTerm,
        List<String> requiredEntitlements,
        boolean includeInactive,
        boolean hideSyntheticOrgnos,
        PageRequest pageRequest
    ) {
        return relyingPartyInstanceRepository.searchRelyingPartyInstances(
            searchTerm,
            requiredEntitlements,
            includeInactive,
            hideSyntheticOrgnos,
            pageRequest
        );
    }

    public RelyingPartyInstance updateRelyingParty(
        UUID id,
        String serviceTradeName,
        boolean active,
        List<RelyingPartyEntitlement> entitlements,
        List<RelyingPartyEaa> eaas
    ) {
        if (id == null) {
            throw new BadRequestException("ID should not be null");
        }

        if (entitlements == null || eaas == null) {
            throw new BadRequestException("Entitlements and EAAs should be emtpy if none exists");
        }

        entitlementCheck(entitlements);

        walletRelyingPartyRepository.findByInstanceIdForUpdate(id)
            .orElseThrow(() -> new BadRequestException("Relying party instance not found"));
        RelyingPartyInstance relyingPartyInstance = relyingPartyInstanceRepository.findById(id)
            .orElseThrow(() -> new BadRequestException("Relying party instance not found"));

        if (active && !relyingPartyInstance.hasActiveRelyingPartyAndService()) {
            throw new BadRequestException("Relying party cannot be activated while its party or service is inactive");
        }

        WalletRelyingPartyService walletService = relyingPartyInstance.getWalletRelyingPartyService();
        if (!Objects.equals(walletService.getServiceTradeName(), serviceTradeName)) {
            walletService.setServiceTradeName(serviceTradeName);
            for (RelyingPartyInstance serviceInstance : walletService.getRelyingPartyInstances()) {
                serviceInstance.setUpdated();
            }
        }
        relyingPartyInstance.setActive(active);

        relyingPartyInstance.setRelyingPartyEntitlements(entitlements);
        relyingPartyInstance.setRelyingPartyEaas(eaas);

        return relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
    }

    public void deactivateWalletRelyingParty(UUID walletRelyingPartyId) {
        WalletRelyingParty walletRelyingParty = findWalletRelyingParty(walletRelyingPartyId);
        walletRelyingParty.setActive(false);
        for (WalletRelyingPartyService walletRelyingPartyService : walletRelyingParty.getServices()) {
            walletRelyingPartyService.setActive(false);
            walletRelyingPartyService.getRelyingPartyInstances()
                .forEach(relyingPartyInstance -> relyingPartyInstance.setActive(false));
        }
        walletRelyingPartyRepository.saveAndFlush(walletRelyingParty);
    }

    public void activateWalletRelyingParty(UUID walletRelyingPartyId) {
        WalletRelyingParty walletRelyingParty = findWalletRelyingParty(walletRelyingPartyId);
        walletRelyingParty.setActive(true);
        walletRelyingPartyRepository.saveAndFlush(walletRelyingParty);
    }

    public void deactivateWalletRelyingPartyService(UUID walletRelyingPartyServiceId) {
        setWalletRelyingPartyServiceActive(walletRelyingPartyServiceId, false);
    }

    public void activateWalletRelyingPartyService(UUID walletRelyingPartyServiceId) {
        setWalletRelyingPartyServiceActive(walletRelyingPartyServiceId, true);
    }

    private void setWalletRelyingPartyServiceActive(UUID walletRelyingPartyServiceId, boolean active) {
        walletRelyingPartyRepository.findByServiceIdForUpdate(walletRelyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));
        WalletRelyingPartyService walletRelyingPartyService =
            walletRelyingPartyServiceRepository.findById(walletRelyingPartyServiceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));

        if (active && !walletRelyingPartyService.getWalletRelyingParty().isActive()) {
            throw new BadRequestException("Service cannot be activated while its party is inactive");
        }

        walletRelyingPartyService.setActive(active);
        if (!active) {
            walletRelyingPartyService.getRelyingPartyInstances()
                .forEach(relyingPartyInstance -> relyingPartyInstance.setActive(false));
        }
        walletRelyingPartyServiceRepository.saveAndFlush(walletRelyingPartyService);
    }

    public void deactivateRelyingPartyInstance(UUID relyingPartyInstanceId) {
        setRelyingPartyInstanceActive(relyingPartyInstanceId, false);
    }

    public void activateRelyingPartyInstance(UUID relyingPartyInstanceId) {
        setRelyingPartyInstanceActive(relyingPartyInstanceId, true);
    }

    private void setRelyingPartyInstanceActive(UUID relyingPartyInstanceId, boolean active) {
        walletRelyingPartyRepository.findByInstanceIdForUpdate(relyingPartyInstanceId)
            .orElseThrow(() -> new NotFoundException("Relying party not found"));
        RelyingPartyInstance relyingPartyInstance = relyingPartyInstanceRepository.findById(relyingPartyInstanceId)
            .orElseThrow(() -> new NotFoundException("Relying party not found"));

        if (active && !relyingPartyInstance.hasActiveRelyingPartyAndService()) {
            throw new BadRequestException("Relying party cannot be activated while its party or service is inactive");
        }

        relyingPartyInstance.setActive(active);
        relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
    }

    private WalletRelyingParty findWalletRelyingParty(UUID walletRelyingPartyId) {
        return walletRelyingPartyRepository.findByIdForUpdate(walletRelyingPartyId)
            .orElseThrow(() -> new NotFoundException("Party not found"));
    }

    public void deleteRelyingParty(UUID id) {
        walletRelyingPartyRepository.findByInstanceIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Relying party not found"));
        RelyingPartyInstance relyingPartyInstance = relyingPartyInstanceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Relying party not found"));
        WalletRelyingPartyService walletService = relyingPartyInstance.getWalletRelyingPartyService();
        walletService.getRelyingPartyInstances().remove(relyingPartyInstance);
        if (walletService.getRelyingPartyInstances().isEmpty()) {
            walletService.getWalletRelyingParty().getServices().remove(walletService);
            walletRelyingPartyServiceRepository.delete(walletService);
        }
    }

    private void entitlementCheck(List<RelyingPartyEntitlement> entitlements) {
        for (RelyingPartyEntitlement entitlement : entitlements) {
            if (!entitlementRepository.existsByEntitlementAndActive(entitlement.getEntitlement(), true)) {
                throw new BadRequestException(entitlement.getEntitlement() + " is not a valid active entitlement");
            }
        }
    }

    public void validateEntitlements(List<RelyingPartyEntitlement> entitlements) {
        if (entitlements == null) {
            throw new BadRequestException("Entitlements must be an empty list when none exist");
        }
        entitlementCheck(entitlements);
    }

    public void validateEntitlementsForUpdate(
        List<RelyingPartyEntitlement> existingEntitlements,
        List<RelyingPartyEntitlement> requestedEntitlements
    ) {
        if (requestedEntitlements == null) {
            throw new BadRequestException("Entitlements must be an empty list when none exist");
        }

        List<RelyingPartyEntitlement> changedEntitlements = requestedEntitlements.stream()
            .filter(requested -> existingEntitlements.stream().noneMatch(existing ->
                Objects.equals(existing.getEntitlement(), requested.getEntitlement())
                    && Objects.equals(existing.getCredentialIssuerUrl(), requested.getCredentialIssuerUrl())))
            .toList();
        entitlementCheck(changedEntitlements);
    }
}

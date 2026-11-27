package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.Entitlement;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
            walletRelyingPartyLookupService.findWalletRelyingPartyForOrgno(orgno)
                .orElseGet(() -> walletRelyingPartyLookupService.createWalletRelyingParty(orgno));

        if (!walletRelyingParty.isUsable()) {
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

    public void revokeRelyingParty(UUID partyId) {
        WalletRelyingParty walletRelyingParty = findWalletRelyingParty(partyId);
        walletRelyingParty.revoke();
        walletRelyingPartyRepository.saveAndFlush(walletRelyingParty);
    }

    public void deactivateRelyingPartyService(UUID serviceId) {
        setRelyingPartyServiceActive(serviceId, false);
    }

    public void activateRelyingPartyService(UUID serviceId) {
        setRelyingPartyServiceActive(serviceId, true);
    }

    private void setRelyingPartyServiceActive(UUID serviceId, boolean active) {
        walletRelyingPartyRepository.findByServiceIdForUpdate(serviceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));
        WalletRelyingPartyService walletRelyingPartyService =
            walletRelyingPartyServiceRepository.findById(serviceId)
            .orElseThrow(() -> new NotFoundException("Service not found"));

        if (active && !walletRelyingPartyService.getWalletRelyingParty().isUsable()) {
            throw new BadRequestException("Service cannot be activated while its party is inactive");
        }

        if (active) {
            walletRelyingPartyService.reactivate();
        } else {
            walletRelyingPartyService.suspend();
        }
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
                Objects.equals(existing.getEntitlement(), requested.getEntitlement())))
            .toList();
        entitlementCheck(changedEntitlements);
    }

    public void validateServiceEntitlements(WalletRelyingPartyService service, List<String> entitlements) {
        validateEntitlementsForUpdate(
            service.getServiceEntitlements().stream().map(RelyingPartyEntitlement::new).toList(),
            entitlements.stream().map(RelyingPartyEntitlement::new).toList());
    }

    public void validateCredentialIssuerUrl(
        WalletRelyingPartyService service, String entitlement, String credentialIssuerUrl
    ) {
        if (!Objects.equals(service.getCredentialIssuerUrl(entitlement), credentialIssuerUrl)) {
            throw new BadRequestException("Use the service API to change a credential issuer URL");
        }
    }

    private void entitlementCheck(List<RelyingPartyEntitlement> entitlements) {
        if (entitlements.isEmpty()) {
            return;
        }

        Set<String> activeEntitlementUris = entitlementRepository.findAllByActive(true).stream()
            .map(Entitlement::getEntitlement)
            .collect(Collectors.toSet());

        for (RelyingPartyEntitlement entitlement : entitlements) {
            if (!activeEntitlementUris.contains(entitlement.getEntitlement())) {
                throw new BadRequestException(entitlement.getEntitlement() + " is not a valid active entitlement");
            }
        }
    }
}

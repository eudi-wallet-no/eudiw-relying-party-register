package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartyService {

    private final EntitlementRepository entitlementRepository;
    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository;
    private final WalletRelyingPartyLookupService walletRelyingPartyLookupService;
    private final WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository;

    @Transactional
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

    @Transactional
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

        RelyingPartyInstance relyingPartyInstance = relyingPartyInstanceRepository.findById(id)
            .orElseThrow(() -> new BadRequestException("Relying party instance not found"));

        WalletRelyingPartyService walletService = relyingPartyInstance.getWalletRelyingPartyService();
        if (!Objects.equals(walletService.getServiceTradeName(), serviceTradeName)) {
            walletService.setServiceTradeName(serviceTradeName);
            for (RelyingPartyInstance serviceInstance : walletService.getRelyingPartyInstances()) {
                serviceInstance.markUpdated();
            }
        }
        relyingPartyInstance.setActive(active);

        relyingPartyInstance.setRelyingPartyEntitlements(entitlements);
        relyingPartyInstance.setRelyingPartyEaas(eaas);

        return relyingPartyInstanceRepository.saveAndFlush(relyingPartyInstance);
    }

    @Transactional
    public void deleteRelyingParty(UUID id) {
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
}

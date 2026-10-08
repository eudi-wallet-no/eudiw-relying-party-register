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
        String orgNr,
        String tradeName,
        List<RelyingPartyEntitlement> entitlements,
        List<RelyingPartyEaa> eaas
    ) {
        if (entitlements == null || eaas == null) {
            throw new BadRequestException("Entitlements and EAAs should be empty if none exists");
        }
        entitlementCheck(entitlements);

        WalletRelyingParty walletRelyingParty = walletRelyingPartyLookupService.getWalletRelyingPartyForOrgno(orgNr);
        if (!walletRelyingParty.isActive()) {
            throw new BadRequestException("Legal entity is not active");
        }

        RelyingPartyInstance relyingPartyInstance =
            new RelyingPartyInstance(entitlements, eaas, new ArrayList<>());
        WalletRelyingPartyService service =
            new WalletRelyingPartyService(tradeName, walletRelyingParty, List.of(relyingPartyInstance));
        walletRelyingParty.getServices().add(service);
        walletRelyingPartyServiceRepository.save(service);

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
        String tradeName,
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

        WalletRelyingPartyService service = relyingPartyInstance.getWalletRelyingPartyService();
        if (!Objects.equals(service.getServiceTradeName(), tradeName)) {
            service.setServiceTradeName(tradeName);
            // v1 shows the instance's last_updated_ms, so a service rename must also update the instance.
            relyingPartyInstance.markUpdated();
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
        WalletRelyingPartyService service = relyingPartyInstance.getWalletRelyingPartyService();
        service.getWalletRelyingParty().getServices().remove(service);
        walletRelyingPartyServiceRepository.delete(service);
    }

    private void entitlementCheck(List<RelyingPartyEntitlement> entitlements) {
        for (RelyingPartyEntitlement entitlement : entitlements) {
            if (!entitlementRepository.existsByEntitlementAndActive(entitlement.getEntitlement(), true)) {
                throw new BadRequestException(entitlement.getEntitlement() + " is not a valid active entitlement");
            }
        }
    }
}

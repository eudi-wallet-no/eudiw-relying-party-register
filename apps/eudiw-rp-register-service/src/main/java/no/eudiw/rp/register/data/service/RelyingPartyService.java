package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.RelyingPartyRegisterServiceException;
import no.eudiw.rp.register.api.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.EditRelyingPartyResource;
import no.eudiw.rp.register.api.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.repository.RelyingPartyEaaRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyEntitlementRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RelyingPartyService {

    private final RelyingPartyRepository relyingPartyRepository;
    private final RelyingPartyEntitlementRepository relyingPartyEntitlementRepository;
    private final RelyingPartyEaaRepository relyingPartyEaaRepository;

    @Transactional
    public RelyingParty createRelyingParty(CreateRelyingPartyResource request) {
        // TODO: Jira EUW-27 (https://digdir.atlassian.net/browse/EUW-27)
        if (relyingPartyRepository.existsByOrgno(request.getOrgNr())) {
            throw new RelyingPartyRegisterServiceException(
                "Orgno already exists in relyingPartyRepository");
        }

        if (request.getRelyingPartyEntitlements() == null || request.getRelyingPartyEaas() == null) {
            throw new RelyingPartyRegisterServiceException(
                    "RelyingPartyEntitlements og RelyingPartyEaas kan ikke være null");
        }

        RelyingParty relyingParty = new RelyingParty(
                request.getName(),
                request.getOrgNr(),
                request.isPublicSector()
        );

        setEntitlementsAndEaa(relyingParty, request.getRelyingPartyEntitlements(), request.getRelyingPartyEaas());

        return relyingPartyRepository.save(relyingParty);
    }

    @Transactional(readOnly = true)
    public RelyingParty findRelyingParty(UUID id) {
        return relyingPartyRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<RelyingParty> findAllRelyingParties() {
        return relyingPartyRepository.findAll();
    }

    @Transactional
    public RelyingParty updateRelyingParty(RelyingParty relyingParty) {
        // updating requires RelyingParty ID to be set; otherwise it is a creation.
        // TODO: Jira EUW-25 (https://digdir.atlassian.net/browse/EUW-25)
        if (relyingParty.getId() == null) {
            throw new RelyingPartyRegisterServiceException(
                "Cannot query update without RelyingParty ID");
        }

        if (!relyingPartyRepository.existsById(relyingParty.getId())) {
            throw new RelyingPartyRegisterServiceException(
                "RelyingParty to update not in relyingPartyRepository");
        }

        return relyingPartyRepository.save(relyingParty);
    }

    @Transactional
    public RelyingParty updateRelyingParty(UUID id, EditRelyingPartyResource request) {
        // updating requires RelyingParty ID to be set; otherwise it is a creation.
        // TODO: Jira EUW-25 (https://digdir.atlassian.net/browse/EUW-25)
        if (id == null) {
            throw new RelyingPartyRegisterServiceException(
                    "Cannot query update without RelyingParty ID");
        }

        if (request.getRelyingPartyEntitlements() == null || request.getRelyingPartyEaas() == null) {
            throw new RelyingPartyRegisterServiceException(
                    "RelyingPartyEntitlements og RelyingPartyEaas kan ikke være null");
        }

        if (!relyingPartyRepository.existsById(id)) {
            throw new RelyingPartyRegisterServiceException(
                    "RelyingParty to update not in relyingPartyRepository");
        }

        RelyingParty relyingParty = relyingPartyRepository.findById(id).orElseThrow();
        relyingParty.setName(request.getName());
        relyingParty.setPublicSector(request.isPublicSector());
        relyingParty.setActive(request.isActive());
        setEntitlementsAndEaa(relyingParty, request.getRelyingPartyEntitlements(), request.getRelyingPartyEaas());

        return relyingPartyRepository.save(relyingParty);
    }

    @Transactional
    public RelyingParty deleteRelyingParty(UUID id) {
        RelyingParty deletedRelyingParty =
            relyingPartyRepository.findById(id).orElse(null);
        relyingPartyRepository.deleteById(id); // does nothing if id does not exist.
        return deletedRelyingParty;
    }

    @Transactional
    public List<RelyingParty> getAllRelyingPartysWithEntitlement(String entitlement) {
        return relyingPartyEntitlementRepository
                   .findAllByEntitlement(entitlement)
                   .stream()
                   .map(RelyingPartyEntitlement::getRelyingParty)
                   .toList();
    }

    private void setEntitlementsAndEaa(
            RelyingParty relyingParty,
            List<RelyingPartyEntitlementResource> entitlements,
            List<RelyingPartyEaaResource> eaas
    ) {
        relyingParty.setRelyingPartyEntitlements(
                entitlements
                        .stream()
                        .map(entitlement -> new RelyingPartyEntitlement(
                                entitlement.getEntitlement(),
                                relyingParty))
                        .collect(Collectors.toList())
        );

        relyingParty.setRelyingPartyEaas(
                eaas
                        .stream()
                        .map(eaa -> new RelyingPartyEaa(
                                eaa.getNamespace(),
                                eaa.getIntent(),
                                relyingParty))
                        .collect(Collectors.toList())
        );
    }
}

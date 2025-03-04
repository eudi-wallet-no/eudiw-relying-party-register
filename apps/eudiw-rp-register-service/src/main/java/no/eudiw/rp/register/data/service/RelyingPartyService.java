package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.RelyingPartyRegisterServiceException;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.repository.RelyingPartyEaaRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyEntitlementRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartyService {

    private final RelyingPartyRepository relyingPartyRepository;
    private final RelyingPartyEntitlementRepository relyingPartyEntitlementRepository;
    private final RelyingPartyEaaRepository relyingPartyEaaRepository;

    @Transactional
    public RelyingParty createRelyingParty(RelyingParty relyingParty) {
        // ID may only be non-null in updates.
        // TODO: Jira EUW-25 (https://digdir.atlassian.net/browse/EUW-25)
        if (relyingParty.getId() != null) {
            throw new RelyingPartyRegisterServiceException(
                "Cannot create RelyingParty with non-null ID");
        }

        // TODO: Jira EUW-27 (https://digdir.atlassian.net/browse/EUW-27)
        if (relyingPartyRepository.existsByOrgno(relyingParty.getOrgno())) {
            throw new RelyingPartyRegisterServiceException(
                "Orgno already exists in relyingPartyRepository");
        }

        return relyingPartyRepository.save(relyingParty);
    }

    @Transactional(readOnly = true)
    public RelyingParty findRelyingParty(UUID id) {
        return relyingPartyRepository.findById(id).orElse(null);
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
}

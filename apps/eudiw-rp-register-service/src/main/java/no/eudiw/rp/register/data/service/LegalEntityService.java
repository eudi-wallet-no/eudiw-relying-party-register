package no.eudiw.rp.register.data.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.repository.LegalEntityRepository;
import no.eudiw.rp.register.data.service.enhetsregisteretservice.EnhetsregisteretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class LegalEntityService {

    private final LegalEntityRepository legalEntityRepository;
    private final EnhetsregisteretService enhetsregisteretService;

    @Transactional
    public LegalEntity getLegalEntityForOrgno(String orgno) {
        LegalEntity legalEntity = legalEntityRepository.findByOrgno(orgno).orElse(null);
        if (legalEntity != null) {
            return legalEntity;
        }

        LegalEntity newLegalEntity = new LegalEntity(
            "Synthetic-legal-entity-%s".formatted(orgno),
            orgno,
            true,
            new ArrayList<>()
        );

        boolean isNonsyntheticOrgno = orgno.matches("^[8-9]\\d{8}$");
        if (isNonsyntheticOrgno) {
            EnhetsregisteretService.EnhetsregisteretResponse enhetsregisteretResponse =
                enhetsregisteretService.queryOrgno(orgno);
            newLegalEntity.setName(enhetsregisteretResponse.name());
            newLegalEntity.setPublicSector(enhetsregisteretResponse.publicSector());
        }

        return legalEntityRepository.save(newLegalEntity);
    }
}

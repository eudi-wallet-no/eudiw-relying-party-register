package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.LegalEntity;
import no.eudiw.rp.register.repository.LegalEntityRepository;
import no.eudiw.rp.register.integrations.enhetsregisteret.EnhetsregisteretService;
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
            "Syntetisk organisasjon %s".formatted(orgno),
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

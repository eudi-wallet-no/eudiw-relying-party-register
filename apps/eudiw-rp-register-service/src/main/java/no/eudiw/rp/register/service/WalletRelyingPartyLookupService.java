package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.exception.AlreadyExistsException;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.integrations.enhetsregisteret.EnhetsregisteretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class WalletRelyingPartyLookupService {

    private final WalletRelyingPartyRepository walletRelyingPartyRepository;
    private final EnhetsregisteretService enhetsregisteretService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<WalletRelyingParty> findWalletRelyingPartyForOrgno(String orgno) {
        return walletRelyingPartyRepository.findByOrgnoForUpdate(orgno);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletRelyingParty createWalletRelyingParty(String orgno) {
        if (walletRelyingPartyRepository.findByOrgnoForUpdate(orgno).isPresent()) {
            throw new AlreadyExistsException("WalletRelyingParty already exists for organization number");
        }

        return createNewWalletRelyingParty(orgno);
    }

    private WalletRelyingParty createNewWalletRelyingParty(String orgno) {
        String name;
        boolean publicSector;
        boolean isSyntheticOrgno = !orgno.matches("^[8-9]\\d{8}$");

        if (isSyntheticOrgno) {
            name = "Syntetisk organisasjon %s".formatted(orgno);
            publicSector = true;
        } else {
            EnhetsregisteretService.EnhetsregisteretResponse enhetsregisteretResponse =
                enhetsregisteretService.queryOrgno(orgno);
            name = enhetsregisteretResponse.name();
            publicSector = enhetsregisteretResponse.publicSector();
        }

        WalletRelyingParty newWalletRelyingParty = new WalletRelyingParty(
            name, orgno, publicSector, List.of());

        return walletRelyingPartyRepository.save(newWalletRelyingParty);
    }
}

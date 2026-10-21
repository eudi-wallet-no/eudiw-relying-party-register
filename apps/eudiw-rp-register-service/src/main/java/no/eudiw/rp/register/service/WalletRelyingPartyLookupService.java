package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.exception.AlreadyExistsException;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.integrations.enhetsregisteret.EnhetsregisteretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class WalletRelyingPartyLookupService {

    private final WalletRelyingPartyRepository walletRelyingPartyRepository;
    private final EnhetsregisteretService enhetsregisteretService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletRelyingParty getWalletRelyingPartyForOrgno(String orgno) {
        WalletRelyingParty walletRelyingParty = walletRelyingPartyRepository.findByOrgnoForUpdate(orgno).orElse(null);
        if (walletRelyingParty != null) {
            return walletRelyingParty;
        }

        return createWalletRelyingParty(orgno);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletRelyingParty createWalletRelyingParty(String orgno) {
        // Take the same orgno lock as getWalletRelyingPartyForOrgno so concurrent creates for the same
        // organization number serialize here instead of failing on the unique constraint.
        if (walletRelyingPartyRepository.findByOrgnoForUpdate(orgno).isPresent()) {
            throw new AlreadyExistsException("WalletRelyingParty already exists for organization number");
        }

        return createNewWalletRelyingParty(orgno);
    }

    private WalletRelyingParty createNewWalletRelyingParty(String orgno) {
        WalletRelyingParty newWalletRelyingParty = new WalletRelyingParty(
            "Syntetisk organisasjon %s".formatted(orgno),
            orgno,
            true,
            new ArrayList<>()
        );
        boolean isNonsyntheticOrgno = orgno.matches("^[8-9]\\d{8}$");
        if (isNonsyntheticOrgno) {
            EnhetsregisteretService.EnhetsregisteretResponse enhetsregisteretResponse =
                enhetsregisteretService.queryOrgno(orgno);
            newWalletRelyingParty.setName(enhetsregisteretResponse.name());
            newWalletRelyingParty.setPSB(enhetsregisteretResponse.publicSector());
        }

        return walletRelyingPartyRepository.save(newWalletRelyingParty);
    }
}

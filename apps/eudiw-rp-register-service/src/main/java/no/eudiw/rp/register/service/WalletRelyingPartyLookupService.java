package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.integrations.enhetsregisteret.EnhetsregisteretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class WalletRelyingPartyLookupService {

    private final WalletRelyingPartyRepository walletRelyingPartyRepository;
    private final EnhetsregisteretService enhetsregisteretService;

    @Transactional
    public WalletRelyingParty getWalletRelyingPartyForOrgno(String orgno) {
        WalletRelyingParty walletRelyingParty = walletRelyingPartyRepository.findByOrgno(orgno).orElse(null);
        if (walletRelyingParty != null) {
            return walletRelyingParty;
        }

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
            newWalletRelyingParty.setLegalName(enhetsregisteretResponse.name());
            newWalletRelyingParty.setPsb(enhetsregisteretResponse.publicSector());
        }

        return walletRelyingPartyRepository.save(newWalletRelyingParty);
    }
}

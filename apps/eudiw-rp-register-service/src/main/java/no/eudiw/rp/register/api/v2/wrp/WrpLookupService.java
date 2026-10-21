package no.eudiw.rp.register.api.v2.wrp;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.config.SupervisoryAuthorityProperties;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WrpLookupService {

    private final WalletRelyingPartyRepository repository;
    private final SupervisoryAuthorityProperties supervisoryAuthorityProperties;
    private final WrpApiProperties wrpApiProperties;

    @Transactional(readOnly = true)
    public Optional<WalletRelyingPartyResource> findByIdentifier(String identifier) {
        List<WalletRelyingParty> matches = repository.findAllByRegisteredIdentifier(identifier);
        if (matches.size() > 1) {
            throw new IllegalStateException("Registered WRP identifier matches more than one party");
        }
        return matches.stream().findFirst()
            .map(party -> WalletRelyingPartyResource.from(
                party, supervisoryAuthorityProperties, wrpApiProperties));
    }
}

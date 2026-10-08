package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CredentialIssuersService {

    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository;

    public Set<String> getRegisteredCredentialIssuerUrls() {
        return relyingPartyInstanceRepository
                .findAll()
                .stream()
                .filter(RelyingPartyInstance::isActive)
                .map(RelyingPartyInstance::getRelyingPartyEntitlements)
                .flatMap(List::stream)
                .map(RelyingPartyEntitlement::getCredentialIssuerUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}

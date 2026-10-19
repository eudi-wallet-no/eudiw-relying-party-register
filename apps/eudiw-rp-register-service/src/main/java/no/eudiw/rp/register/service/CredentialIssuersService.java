package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import org.springframework.stereotype.Service;

import java.util.Set;

@RequiredArgsConstructor
@Service
public class CredentialIssuersService {

    private final RelyingPartyInstanceRepository relyingPartyInstanceRepository;

    public Set<String> getRegisteredCredentialIssuerUrls() {
        return relyingPartyInstanceRepository.findActiveCredentialIssuerUrls();
    }
}

package no.eudiw.rp.register.service;

import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("When using the credential issuers service")
@ActiveProfiles("junit")
public class CredentialIssuersServiceTest {

    @MockitoBean
    @SuppressWarnings("unused")
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;

    @Autowired
    private CredentialIssuersService credentialIssuersService;

    @Nested
    @DisplayName("when fetching list of registered issuer URLs")
    class GetRegisteredCredentialIssuerUrlsTests {
        @Test
        @DisplayName("then registered issuer URLs are returned from the repository")
        void returnsRegisteredIssuerUrls() {
            Set<String> expectedIssuerUrls = Set.of("https://issuer.example.com");
            when(relyingPartyInstanceRepository.findActiveCredentialIssuerUrls())
                .thenReturn(expectedIssuerUrls);

            Set<String> issuerUrls = credentialIssuersService.getRegisteredCredentialIssuerUrls();

            assertEquals(expectedIssuerUrls, issuerUrls);
            verify(relyingPartyInstanceRepository).findActiveCredentialIssuerUrls();
        }
    }
}

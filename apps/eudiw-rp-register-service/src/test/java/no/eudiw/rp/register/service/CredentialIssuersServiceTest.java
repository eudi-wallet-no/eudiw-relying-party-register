package no.eudiw.rp.register.service;

import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        @DisplayName("then only issuer URLs for active RP instances are returned")
        void testInactiveInstanceIssuerUrlsNotIncludedInResult() {
            RelyingPartyInstance activeRelyingPartyInstance = EntityGenerator.generateRelyingParty();
            RelyingPartyInstance inactiveRelyingPartyInstance = EntityGenerator.generateRelyingParty();

            String inactiveInstanceIssuerUrl = TestDataGenerator.generateIssuerUrl();
            inactiveRelyingPartyInstance.setActive(false);
            inactiveRelyingPartyInstance.setRelyingPartyEntitlements(List.of(
                new RelyingPartyEntitlement("foo", inactiveInstanceIssuerUrl)));

            when(relyingPartyInstanceRepository.findAll())
                .thenReturn(List.of(activeRelyingPartyInstance, inactiveRelyingPartyInstance));

            Set<String> expectedIssuerUrls =
                activeRelyingPartyInstance
                    .getRelyingPartyEntitlements()
                    .stream()
                    .map(RelyingPartyEntitlement::getCredentialIssuerUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<String> issuerUrls = new HashSet<>(
                credentialIssuersService.getRegisteredCredentialIssuerUrls());
            verify(relyingPartyInstanceRepository, times(1)).findAll();

            assertEquals(expectedIssuerUrls, issuerUrls);
            assertFalse(issuerUrls.contains(inactiveInstanceIssuerUrl));
        }

        @Test
        @DisplayName("then null issuer URLs are not included in result")
        void testNullIssuerUrlsAreNotIncludedInResult() {
            RelyingPartyInstance relyingPartyInstance1 = EntityGenerator.generateRelyingParty();
            RelyingPartyInstance relyingPartyInstance2 = EntityGenerator.generateRelyingParty();

            String issuerUrl = TestDataGenerator.generateIssuerUrl();
            relyingPartyInstance1.setRelyingPartyEntitlements(List.of(
                new RelyingPartyEntitlement("foo", issuerUrl)));
            relyingPartyInstance2.setRelyingPartyEntitlements(List.of(
                new RelyingPartyEntitlement("bar", null)));

            when(relyingPartyInstanceRepository.findAll())
                .thenReturn(List.of(relyingPartyInstance1, relyingPartyInstance2));

            Set<String> issuerUrls =
                credentialIssuersService.getRegisteredCredentialIssuerUrls();

            verify(relyingPartyInstanceRepository, times(1)).findAll();

            assertEquals(1, issuerUrls.size());
            assertTrue(issuerUrls.contains(issuerUrl));
            assertFalse(issuerUrls.contains(null));
        }
    }
}

package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.api.v1.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.v1.V1ApiService;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.integrations.certificateservice.CertificateServiceClient;
import no.eudiw.rp.register.repository.AccessCertificateRepository;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import no.eudiw.rp.register.service.RelyingPartyCertificateService;
import no.eudiw.rp.register.service.RelyingPartyService;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V2RegistrationStatusIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_KEY = "junit-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @Autowired
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;

    @Autowired
    private WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository;

    @Autowired
    private AccessCertificateRepository accessCertificateRepository;

    @Autowired
    private IssuerCertificateRepository issuerCertificateRepository;

    @Autowired
    private RelyingPartyCertificateService relyingPartyCertificateService;

    @Autowired
    private V1ApiService v1ApiService;

    @Autowired
    private RelyingPartyService relyingPartyService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @MockitoBean
    private CertificateServiceClient certificateServiceClient;

    @BeforeEach
    void clearRepositoryBeforeEachTest() {
        accessCertificateRepository.deleteAll();
        issuerCertificateRepository.deleteAll();
        relyingPartyInstanceRepository.deleteAll();
        walletRelyingPartyRepository.deleteAll();
    }

    @Test
    void registrationLevelsCanBeActivatedAndDeactivatedIndependently() throws Exception {
        Registration registration = createRegistration();

        changeRegistrationStatus("/v2/instances/%s/deactivate".formatted(registration.relyingPartyInstance().getId()));

        assertRegistrationStatus(registration, true, true, false, true, true, true);

        changeRegistrationStatus("/v2/services/%s/deactivate".formatted(registration.walletRelyingPartyService().getId()));

        assertRegistrationStatus(registration, true, false, false, false, true, true);

        changeRegistrationStatus("/v2/services/%s/activate".formatted(registration.walletRelyingPartyService().getId()));

        assertRegistrationStatus(registration, true, true, false, false, true, true);

        changeRegistrationStatus("/v2/instances/%s/activate".formatted(registration.relyingPartyInstance().getId()));

        assertRegistrationStatus(registration, true, true, true, false, true, true);

        changeRegistrationStatus("/v2/parties/%s/deactivate".formatted(registration.walletRelyingParty().getId()));

        assertRegistrationStatus(registration, false, false, false, false, false, false);

        changeRegistrationStatus("/v2/parties/%s/activate".formatted(registration.walletRelyingParty().getId()));

        assertRegistrationStatus(registration, true, false, false, false, false, false);

        changeRegistrationStatus("/v2/services/%s/activate".formatted(registration.otherWalletRelyingPartyService().getId()));

        assertRegistrationStatus(registration, true, false, false, false, true, false);

        changeRegistrationStatus("/v2/instances/%s/activate".formatted(registration.otherRelyingPartyInstance().getId()));

        assertRegistrationStatus(registration, true, false, false, false, true, true);

        changeRegistrationStatus("/v2/services/%s/activate".formatted(registration.walletRelyingPartyService().getId()));

        assertRegistrationStatus(registration, true, true, false, false, true, true);

        changeRegistrationStatus("/v2/instances/%s/activate".formatted(registration.siblingRelyingPartyInstance().getId()));

        assertRegistrationStatus(registration, true, true, false, true, true, true);
    }

    @Test
    void cannotReactivateChildWhileAnyAncestorIsInactive() throws Exception {
        Registration registration = createRegistration();
        changeRegistrationStatus("/v2/parties/%s/deactivate".formatted(registration.walletRelyingParty().getId()));

        mockMvc.perform(patch("/v2/services/%s/activate".formatted(registration.walletRelyingPartyService().getId()))
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());
        assertFalse(isWalletRelyingPartyServiceActive(registration));

        changeRegistrationStatus("/v2/parties/%s/activate".formatted(registration.walletRelyingParty().getId()));

        mockMvc.perform(patch("/v2/instances/%s/activate".formatted(registration.relyingPartyInstance().getId()))
                .header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isBadRequest());

        assertTrue(isWalletRelyingPartyActive(registration));
        assertFalse(isWalletRelyingPartyServiceActive(registration));
        assertFalse(isRelyingPartyInstanceActive(registration));
    }

    @ParameterizedTest
    @EnumSource(RegistrationLevel.class)
    void rejectsCertificateIssuanceWhenAnyRegistrationLevelIsInactive(RegistrationLevel level) throws Exception {
        Registration registration = createRegistration();
        String path = switch (level) {
            case WALLET_RELYING_PARTY -> "/v2/parties/%s/deactivate".formatted(registration.walletRelyingParty().getId());
            case WALLET_RELYING_PARTY_SERVICE -> "/v2/services/%s/deactivate".formatted(registration.walletRelyingPartyService().getId());
            case RELYING_PARTY_INSTANCE -> "/v2/instances/%s/deactivate".formatted(registration.relyingPartyInstance().getId());
        };
        changeRegistrationStatus(path);

        assertCertificateRequestsRejected(registration);
    }

    @ParameterizedTest
    @EnumSource(value = RegistrationLevel.class, names = {"WALLET_RELYING_PARTY", "WALLET_RELYING_PARTY_SERVICE"})
    void rejectsCertificateIssuanceForActiveInstanceWithInactiveAncestor(RegistrationLevel level) throws Exception {
        Registration registration = createRegistration();
        if (level == RegistrationLevel.WALLET_RELYING_PARTY) {
            jdbcTemplate.update("UPDATE wallet_relying_party SET active = FALSE WHERE id = ?",
                registration.walletRelyingParty().getId().toString());
        } else {
            jdbcTemplate.update("UPDATE wallet_relying_party_service SET active = FALSE WHERE id = ?",
                registration.walletRelyingPartyService().getId().toString());
        }
        assertTrue(isRelyingPartyInstanceActive(registration));
        assertCertificateRequestsRejected(registration);
    }

    @ParameterizedTest
    @EnumSource(CertificateKind.class)
    void certificateIssuanceWaitsForHierarchyWriteBeforeCallingCertificateAuthority(CertificateKind kind)
        throws Exception {
        Registration registration = createRegistration();
        entitlementRepository.saveAndFlush(new Entitlement(
            registration.entitlement().getEntitlement(), true, "Issuer", "test-ca"));
        var partyLocked = new CountDownLatch(1);
        var allowPartyCommit = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var partyWrite = executor.submit(() -> transaction().executeWithoutResult(status -> {
                relyingPartyService.deactivateWalletRelyingParty(registration.walletRelyingParty().getId());
                partyLocked.countDown();
                await(allowPartyCommit);
            }));
            assertTrue(partyLocked.await(10, TimeUnit.SECONDS));

            var certificateRequestStarted = new CountDownLatch(1);
            var issuance = executor.submit(() -> {
                certificateRequestStarted.countDown();
                assertThrows(NotFoundException.class, () -> requestCertificate(registration, kind));
            });
            try {
                assertTrue(certificateRequestStarted.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> issuance.get(500, TimeUnit.MILLISECONDS));
            } finally {
                allowPartyCommit.countDown();
            }

            partyWrite.get(10, TimeUnit.SECONDS);
            issuance.get(10, TimeUnit.SECONDS);
        }

        verifyNoInteractions(certificateServiceClient);
    }

    @ParameterizedTest
    @EnumSource(RegistrationLevel.class)
    void deactivationPreservesBothUnrevokedAndAlreadyRevokedCertificates(RegistrationLevel level) throws Exception {
        Registration registration = createRegistration();
        registration.issuerCertificate().revoke(1);
        issuerCertificateRepository.saveAndFlush(registration.issuerCertificate());
        String path = switch (level) {
            case WALLET_RELYING_PARTY -> "/v2/parties/%s/deactivate".formatted(registration.walletRelyingParty().getId());
            case WALLET_RELYING_PARTY_SERVICE -> "/v2/services/%s/deactivate".formatted(registration.walletRelyingPartyService().getId());
            case RELYING_PARTY_INSTANCE -> "/v2/instances/%s/deactivate".formatted(registration.relyingPartyInstance().getId());
        };

        changeRegistrationStatus(path);

        assertEquals(-1, relyingPartyCertificateService.getCertificate(
            registration.accessCertificate().getId(), registration.relyingPartyInstance().getId()).getRevocationStatus());
        assertEquals(1, relyingPartyCertificateService.getIssuerCertificate(
            registration.issuerCertificate().getId(), registration.relyingPartyInstance().getId()).getRevocationStatus());
        verifyNoInteractions(certificateServiceClient);
    }

    @ParameterizedTest
    @EnumSource(RegistrationLevel.class)
    void v2DoesNotOfferPhysicalDeletion(RegistrationLevel level) throws Exception {
        Registration registration = createRegistration();
        String path = switch (level) {
            case WALLET_RELYING_PARTY -> "/v2/parties/%s".formatted(registration.walletRelyingParty().getId());
            case WALLET_RELYING_PARTY_SERVICE -> "/v2/services/%s".formatted(registration.walletRelyingPartyService().getId());
            case RELYING_PARTY_INSTANCE -> "/v2/instances/%s".formatted(registration.relyingPartyInstance().getId());
        };

        mockMvc.perform(delete(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isMethodNotAllowed());

        assertTrue(isWalletRelyingPartyActive(registration));
        assertTrue(isWalletRelyingPartyServiceActive(registration));
        assertTrue(isRelyingPartyInstanceActive(registration));
        assertTrue(accessCertificateRepository.existsById(registration.accessCertificate().getId()));
        assertTrue(issuerCertificateRepository.existsById(registration.issuerCertificate().getId()));
    }

    @Test
    void explicitlyRevokesCertificatesForInactiveRegistrationThroughCertificateAuthority() throws Exception {
        Registration registration = createRegistration();
        changeRegistrationStatus("/v2/parties/%s/deactivate".formatted(registration.walletRelyingParty().getId()));
        when(certificateServiceClient.revokeCertificate(anyString(), anyInt(), anyString()))
            .thenReturn(HttpStatus.NO_CONTENT);

        relyingPartyCertificateService.revokeAccessCertificate(
            registration.accessCertificate().getId(), registration.relyingPartyInstance().getId());
        relyingPartyCertificateService.revokeIssuerCertificate(
            registration.issuerCertificate().getId(), registration.relyingPartyInstance().getId());

        assertEquals(0, accessCertificateRepository.findById(registration.accessCertificate().getId())
            .orElseThrow().getRevocationStatus());
        assertEquals(0, issuerCertificateRepository.findById(registration.issuerCertificate().getId())
            .orElseThrow().getRevocationStatus());
        verify(certificateServiceClient, times(2)).revokeCertificate(anyString(), anyInt(), anyString());
    }

    @Test
    void requiresApiKeyForV2StatusOperations() throws Exception {
        mockMvc.perform(patch("/v2/parties/%s/deactivate".formatted(java.util.UUID.randomUUID())))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(certificateServiceClient);
    }

    private void changeRegistrationStatus(String path) throws Exception {
        mockMvc.perform(patch(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isNoContent());
    }

    private void assertCertificateRequestsRejected(Registration registration) throws Exception {
        var certificateSigningRequest = CertificatesGenerator.generatePKCS10Csr();
        var objectMapper = new ObjectMapper();
        long accessCertificateCount = accessCertificateRepository.count();
        long issuerCertificateCount = issuerCertificateRepository.count();
        String certificatePath = "/v1/rp/%s/certs".formatted(registration.relyingPartyInstance().getId());

        mockMvc.perform(post(certificatePath + "/access")
                .header(API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RelyingPartyCsrResource(certificateSigningRequest))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("not_found"))
            .andExpect(jsonPath("$.error_description")
                .value("Certificate registree does not exist or is inactive"));
        mockMvc.perform(post(certificatePath + "/issuer")
                .header(API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new IssuerCsrResource(
                    certificateSigningRequest, registration.entitlement().getEntitlement()))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("not_found"))
            .andExpect(jsonPath("$.error_description")
                .value("Certificate registree does not exist or is inactive"));

        assertEquals(accessCertificateCount, accessCertificateRepository.count());
        assertEquals(issuerCertificateCount, issuerCertificateRepository.count());
        verifyNoInteractions(certificateServiceClient);
    }

    private void requestCertificate(Registration registration, CertificateKind kind) {
        var csr = CertificatesGenerator.generatePKCS10Csr();
        if (kind == CertificateKind.ACCESS) {
            v1ApiService.requestAccessCertificateForRelyingParty(
                registration.relyingPartyInstance().getId(), new RelyingPartyCsrResource(csr));
            return;
        }
        v1ApiService.requestIssuerCertificate(registration.relyingPartyInstance().getId(),
            new IssuerCsrResource(csr, registration.entitlement().getEntitlement()));
    }

    private boolean isWalletRelyingPartyActive(Registration registration) {
        return walletRelyingPartyRepository.findById(registration.walletRelyingParty().getId()).orElseThrow().isActive();
    }

    private boolean isWalletRelyingPartyServiceActive(Registration registration) {
        return walletRelyingPartyServiceRepository.findById(registration.walletRelyingPartyService().getId()).orElseThrow().isActive();
    }

    private boolean isRelyingPartyInstanceActive(Registration registration) {
        return relyingPartyInstanceRepository.findById(registration.relyingPartyInstance().getId()).orElseThrow().isActive();
    }

    private boolean isRelyingPartyInstanceActive(RelyingPartyInstance relyingPartyInstance) {
        return relyingPartyInstanceRepository.findById(relyingPartyInstance.getId()).orElseThrow().isActive();
    }

    private void assertRegistrationStatus(
        Registration registration,
        boolean walletRelyingPartyActive,
        boolean walletRelyingPartyServiceActive,
        boolean relyingPartyInstanceActive,
        boolean siblingRelyingPartyInstanceActive,
        boolean otherWalletRelyingPartyServiceActive,
        boolean otherRelyingPartyInstanceActive
    ) {
        assertEquals(walletRelyingPartyActive, isWalletRelyingPartyActive(registration));
        assertEquals(walletRelyingPartyServiceActive, isWalletRelyingPartyServiceActive(registration));
        assertEquals(relyingPartyInstanceActive, isRelyingPartyInstanceActive(registration));
        assertEquals(siblingRelyingPartyInstanceActive,
            isRelyingPartyInstanceActive(registration.siblingRelyingPartyInstance()));
        assertEquals(otherWalletRelyingPartyServiceActive,
            walletRelyingPartyServiceRepository.findById(registration.otherWalletRelyingPartyService().getId())
                .orElseThrow().isActive());
        assertEquals(otherRelyingPartyInstanceActive,
            isRelyingPartyInstanceActive(registration.otherRelyingPartyInstance()));
    }

    private Registration createRegistration() {
        var entitlement = new RelyingPartyEntitlement("urn:test:issuer:" + UUID.randomUUID());
        var relyingPartyInstance = new RelyingPartyInstance(List.of(entitlement), List.of(), List.of());
        var siblingRelyingPartyInstance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var walletRelyingPartyService = new WalletRelyingPartyService(
            "Service", null, List.of(relyingPartyInstance, siblingRelyingPartyInstance));
        var otherRelyingPartyInstance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var otherWalletRelyingPartyService = new WalletRelyingPartyService(
            "Other service", null, List.of(otherRelyingPartyInstance));
        var walletRelyingParty = new WalletRelyingParty(
            "Party", TestDataGenerator.generateValidOrgno(), false, List.of(walletRelyingPartyService, otherWalletRelyingPartyService));
        var accessCertificate = new AccessCertificate(
            "test-ca", CertificatesGenerator.generateX509Certificate(), relyingPartyInstance);
        relyingPartyInstance.setAccessCertificates(List.of(accessCertificate));
        var issuerCertificate = new IssuerCertificate(
            CertificatesGenerator.generateX509Certificate(), "test-ca", entitlement);
        entitlement.addIssuerCertificate(issuerCertificate);
        walletRelyingPartyRepository.saveAndFlush(walletRelyingParty);

        return new Registration(
            walletRelyingParty,
            walletRelyingPartyService,
            relyingPartyInstance,
            siblingRelyingPartyInstance,
            otherWalletRelyingPartyService,
            otherRelyingPartyInstance,
            entitlement,
            accessCertificate,
            issuerCertificate
        );
    }

    private TransactionTemplate transaction() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return transaction;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to commit");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private record Registration(
        WalletRelyingParty walletRelyingParty,
        WalletRelyingPartyService walletRelyingPartyService,
        RelyingPartyInstance relyingPartyInstance,
        RelyingPartyInstance siblingRelyingPartyInstance,
        WalletRelyingPartyService otherWalletRelyingPartyService,
        RelyingPartyInstance otherRelyingPartyInstance,
        RelyingPartyEntitlement entitlement,
        AccessCertificate accessCertificate,
        IssuerCertificate issuerCertificate
    ) { }

    private enum RegistrationLevel {
        WALLET_RELYING_PARTY,
        WALLET_RELYING_PARTY_SERVICE,
        RELYING_PARTY_INSTANCE
    }

    private enum CertificateKind {
        ACCESS,
        ISSUER
    }

}

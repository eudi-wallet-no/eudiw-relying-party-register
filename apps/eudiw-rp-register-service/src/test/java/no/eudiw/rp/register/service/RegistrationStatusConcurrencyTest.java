package no.eudiw.rp.register.service;

import no.eudiw.rp.register.api.v2.V2ApiService;
import no.eudiw.rp.register.api.v2.endpoints.entities.CreateInstanceRequest;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
class RegistrationStatusConcurrencyTest {

    private static final String ENTITLEMENT = "urn:test:concurrent-activation";

    @Autowired
    private RelyingPartyService relyingPartyService;

    @Autowired
    private V2ApiService v2ApiService;

    @Autowired
    private WalletRelyingPartyRepository partyRepository;

    @Autowired
    private WalletRelyingPartyServiceRepository serviceRepository;

    @Autowired
    private RelyingPartyInstanceRepository instanceRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        partyRepository.deleteAll();
        entitlementRepository.findByEntitlement(ENTITLEMENT).ifPresent(entitlementRepository::delete);
        entitlementRepository.saveAndFlush(new Entitlement(ENTITLEMENT, true, "Test", "test-ca"));
    }

    @Test
    void setupPreservesSeededEntitlements() {
        assertTrue(entitlementRepository.existsByEntitlementAndActive(
            "https://uri.etsi.org/19475/Entitlement/QEAA_Provider", true));
    }

    @ParameterizedTest
    @EnumSource(HierarchyWrite.class)
    @DisplayName("Should reject a child write after an ancestor status change")
    void shouldRejectAChildWriteAfterAnAncestorStatusChange(HierarchyWrite write) throws Exception {
        Registration registration = createRegistration(write);
        var deactivated = new CountDownLatch(1);
        var allowCommit = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> deactivation = executor.submit(() -> transaction().executeWithoutResult(status -> {
                deactivateAncestor(registration, write);
                deactivated.countDown();
                await(allowCommit);
            }));
            try {
                assertTrue(deactivated.await(10, TimeUnit.SECONDS));
                var writeStarted = new CountDownLatch(1);
                Future<?> activation = executor.submit(() -> {
                    writeStarted.countDown();
                    assertThrows(BadRequestException.class, () -> activateChild(registration, write));
                });
                assertTrue(writeStarted.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> activation.get(500, TimeUnit.MILLISECONDS));

                allowCommit.countDown();
                deactivation.get(10, TimeUnit.SECONDS);
                activation.get(10, TimeUnit.SECONDS);
            } finally {
                allowCommit.countDown();
            }
        }

        assertChildrenAfterRevocation(registration, write, false);
        assertEquals(1, serviceRepository.count());
    }

    @ParameterizedTest
    @EnumSource(HierarchyWrite.class)
    @DisplayName("Should serialize an ancestor status change after a child write")
    void shouldSerializeAnAncestorStatusChangeAfterAChildWrite(HierarchyWrite write) throws Exception {
        Registration registration = createRegistration(write);
        var activated = new CountDownLatch(1);
        var allowCommit = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> activation = executor.submit(() -> transaction().executeWithoutResult(status -> {
                activateChild(registration, write);
                activated.countDown();
                await(allowCommit);
            }));
            try {
                assertTrue(activated.await(10, TimeUnit.SECONDS));
                var deactivationStarted = new CountDownLatch(1);
                Future<?> deactivation = executor.submit(() -> {
                    deactivationStarted.countDown();
                    deactivateAncestor(registration, write);
                });
                assertTrue(deactivationStarted.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> deactivation.get(500, TimeUnit.MILLISECONDS));

                allowCommit.countDown();
                activation.get(10, TimeUnit.SECONDS);
                deactivation.get(10, TimeUnit.SECONDS);
            } finally {
                allowCommit.countDown();
            }
        }

        assertChildrenAfterRevocation(registration, write, true);
        assertEquals(write == HierarchyWrite.INSTANCE_CREATION ? 2 : 1, serviceRepository.count());
    }

    private Registration createRegistration(HierarchyWrite write) {
        var instance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        instance.setActive(false);
        var service = new WalletRelyingPartyService("Service", null, List.of(instance));
        service.setEntitlements(List.of(ENTITLEMENT));
        service.setActive(write != HierarchyWrite.SERVICE_ACTIVATION);
        var party = new WalletRelyingParty("Party", "123456789", false, List.of(service));
        partyRepository.saveAndFlush(party);
        return new Registration(party.getId(), service.getId(), instance.getId(), party.getOrgno());
    }

    private void activateChild(Registration registration, HierarchyWrite write) {
        switch (write) {
            case SERVICE_ACTIVATION -> relyingPartyService.activateRelyingPartyService(registration.serviceId());
            case INSTANCE_ACTIVATION -> relyingPartyService.activateRelyingPartyInstance(registration.instanceId());
            case INSTANCE_CREATION -> v2ApiService.createInstance(new CreateInstanceRequest(
                registration.orgno(), "New service", List.of(new CreateInstanceRequest.EntitlementRequest(ENTITLEMENT))));
        }
    }

    private void deactivateAncestor(Registration registration, HierarchyWrite write) {
        if (write == HierarchyWrite.INSTANCE_ACTIVATION) {
            relyingPartyService.deactivateRelyingPartyService(registration.serviceId());
        } else {
            relyingPartyService.revokeRelyingParty(registration.partyId());
        }
    }

    private void assertChildrenAfterRevocation(Registration registration, HierarchyWrite write, boolean childWriteSucceeded) {
        var party = partyRepository.findById(registration.partyId()).orElseThrow();
        if (write == HierarchyWrite.INSTANCE_ACTIVATION) {
            assertNull(party.getRevokedAt());
            assertFalse(instanceRepository.findById(registration.instanceId()).orElseThrow().isActive());
        } else {
            assertNotNull(party.getRevokedAt());
            assertFalse(instanceRepository.findById(registration.instanceId()).orElseThrow().isActive());
            assertEquals(write == HierarchyWrite.INSTANCE_CREATION || childWriteSucceeded,
                serviceRepository.findById(registration.serviceId()).orElseThrow().isActive());
        }
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

    private record Registration(UUID partyId, UUID serviceId, UUID instanceId, String orgno) { }

    private enum HierarchyWrite {
        SERVICE_ACTIVATION,
        INSTANCE_ACTIVATION,
        INSTANCE_CREATION
    }
}

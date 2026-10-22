package no.eudiw.rp.register.service;

import no.eudiw.rp.register.api.v1.V1ApiService;
import no.eudiw.rp.register.api.v1.resource.relyingparty.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.EditRelyingPartyResource;
import no.eudiw.rp.register.api.v1.resource.relyingparty.RelyingPartyEntitlementResource;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    private V1ApiService v1ApiService;

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
    void childWriteWaitsForDeactivationAndRejectsInactiveAncestor(HierarchyWrite write) throws Exception {
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

        reactivateAncestor(registration, write);

        assertChildrenInactive(registration, write);
        assertEquals(1, serviceRepository.count());
    }

    @ParameterizedTest
    @EnumSource(HierarchyWrite.class)
    void deactivationWaitsForChildWriteAndThenCascadesToIt(HierarchyWrite write) throws Exception {
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

        reactivateAncestor(registration, write);

        assertChildrenInactive(registration, write);
        assertEquals(write == HierarchyWrite.V1_CREATION ? 2 : 1, serviceRepository.count());
    }

    private Registration createRegistration(HierarchyWrite write) {
        var instance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        instance.setActive(false);
        var service = new WalletRelyingPartyService("Service", null, List.of(instance));
        service.setActive(write != HierarchyWrite.SERVICE_ACTIVATION);
        var party = new WalletRelyingParty("Party", "123456789", false, List.of(service));
        partyRepository.saveAndFlush(party);
        return new Registration(party.getId(), service.getId(), instance.getId(), party.getOrgno());
    }

    private void activateChild(Registration registration, HierarchyWrite write) {
        var entitlements = List.of(new RelyingPartyEntitlementResource(ENTITLEMENT, null, null));
        switch (write) {
            case SERVICE_ACTIVATION -> relyingPartyService.activateRelyingPartyService(registration.serviceId());
            case INSTANCE_ACTIVATION -> relyingPartyService.activateRelyingPartyInstance(registration.instanceId());
            case V1_UPDATE_SERVICE, V1_UPDATE_PARTY -> v1ApiService.updateRelyingParty(
                registration.instanceId(), new EditRelyingPartyResource("Service", entitlements, List.of(), true));
            case V1_CREATION -> v1ApiService.createRelyingParty(
                new CreateRelyingPartyResource(registration.orgno(), "New service", entitlements, List.of()));
        }
    }

    private void deactivateAncestor(Registration registration, HierarchyWrite write) {
        if (write == HierarchyWrite.INSTANCE_ACTIVATION || write == HierarchyWrite.V1_UPDATE_SERVICE) {
            relyingPartyService.deactivateRelyingPartyService(registration.serviceId());
        } else {
            relyingPartyService.deactivateRelyingParty(registration.partyId());
        }
    }

    private void reactivateAncestor(Registration registration, HierarchyWrite write) {
        if (write == HierarchyWrite.INSTANCE_ACTIVATION || write == HierarchyWrite.V1_UPDATE_SERVICE) {
            relyingPartyService.activateRelyingPartyService(registration.serviceId());
        } else {
            relyingPartyService.activateRelyingParty(registration.partyId());
        }
    }

    private void assertChildrenInactive(Registration registration, HierarchyWrite write) {
        assertTrue(instanceRepository.findAll().stream().noneMatch(RelyingPartyInstance::isActive));
        assertTrue(partyRepository.findById(registration.partyId()).orElseThrow().isActive());
        if (write != HierarchyWrite.INSTANCE_ACTIVATION && write != HierarchyWrite.V1_UPDATE_SERVICE) {
            assertTrue(serviceRepository.findAll().stream().noneMatch(WalletRelyingPartyService::isActive));
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
        V1_UPDATE_SERVICE,
        V1_UPDATE_PARTY,
        V1_CREATION
    }
}

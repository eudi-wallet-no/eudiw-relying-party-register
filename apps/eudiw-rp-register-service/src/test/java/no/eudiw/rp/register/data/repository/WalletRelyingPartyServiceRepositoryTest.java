package no.eudiw.rp.register.data.repository;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.domain.IntendedUse;
import no.eudiw.rp.register.domain.IntermediaryReference;
import no.eudiw.rp.register.domain.MultiLangString;
import no.eudiw.rp.register.domain.Policy;
import no.eudiw.rp.register.domain.ProvidedAttestation;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static no.eudiw.rp.register.testdata.TestDataGenerator.generateValidOrgno;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When using WalletRelyingPartyServiceRepository")
@ActiveProfiles("junit")
class WalletRelyingPartyServiceRepositoryTest {

    @Autowired
    private WalletRelyingPartyServiceRepository repository;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @Autowired
    private RelyingPartyInstanceRepository instanceRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        walletRelyingPartyRepository.deleteAll();
    }

    @Test
    @DisplayName("persists the service, its relying party and its instance")
    void persistsServiceGraph() {
        var instance = new RelyingPartyInstance(
            List.of(new RelyingPartyEntitlement("urn:test:service-provider")),
            List.of(),
            List.of());
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service Trade Name", relyingParty, List.of(instance));
        service.setServiceIdentifier("payments");
        service.setSupportUri("https://service.example/support");
        service.setEmail("support@service.example");
        service.setPhone("+4712345678");
        service.setSrvDescription(List.of(List.of(
            new MultiLangString("en", "Payment service"),
            new MultiLangString("nb", "Betalingstjeneste"))));
        service.setSubEntitlements(List.of("urn:test:payment"));
        service.setProvidesAttestations(List.of(
            new ProvidedAttestation("dc+sd-jwt", "https://schemas.example/payment")));
        relyingParty.setServices(List.of(service));

        WalletRelyingPartyService savedService = repository.saveAndFlush(service);
        entityManager.clear();

        WalletRelyingPartyService actual = repository.findById(savedService.getId()).orElseThrow();
        RelyingPartyInstance actualInstance = actual.getRelyingPartyInstances().getFirst();

        assertAll(
            () -> assertEquals("Service Trade Name", actual.getServiceTradeName()),
            () -> assertEquals("payments", actual.getServiceIdentifier()),
            () -> assertEquals("https://service.example/support", actual.getSupportUri()),
            () -> assertEquals("support@service.example", actual.getEmail()),
            () -> assertEquals("+4712345678", actual.getPhone()),
            () -> assertEquals(List.of(List.of(
                new MultiLangString("en", "Payment service"),
                new MultiLangString("nb", "Betalingstjeneste"))), actual.getSrvDescription()),
            () -> assertEquals(List.of("urn:test:payment"), actual.getSubEntitlements()),
            () -> assertEquals(List.of(
                new ProvidedAttestation("dc+sd-jwt", "https://schemas.example/payment")),
                actual.getProvidesAttestations()),
            () -> assertFalse(intermediaryFlag(actual.getId())),
            () -> assertEquals(List.of(), actual.getUsesIntermediaries()),
            () -> assertEquals(List.of(), actual.getServedWRPServices()),
            () -> assertEquals(List.of(), actual.getIntendedUses()),
            () -> assertEquals(List.of("urn:test:service-provider"), actual.getEntitlements()),
            () -> assertEquals(relyingParty.getId(), actual.getWalletRelyingParty().getId()),
            () -> assertEquals("Legal Name", actual.getWalletRelyingParty().getTradeName()),
            () -> assertEquals(1, actual.getRelyingPartyInstances().size()),
            () -> assertEquals(actual.getId(), actualInstance.getWalletRelyingPartyService().getId()),
            () -> assertTrue(actual.getCreatedMs() > 0),
            () -> assertTrue(actual.getLastUpdatedMs() > 0)
        );
    }

    @Test
    @DisplayName("persists intermediary relationships and intended uses")
    void persistsIntermediaryRelationshipsAndIntendedUses() {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var intermediaryService = new WalletRelyingPartyService("Intermediary", relyingParty, List.of());
        intermediaryService.setServiceIdentifier("intermediary-service");
        intermediaryService.setIntermediaryConfiguration(true, List.of(), List.of("client-service"));

        var clientService = new WalletRelyingPartyService("Client", relyingParty, List.of());
        clientService.setServiceIdentifier("client-service");
        clientService.setIntermediaryConfiguration(false, List.of(new IntermediaryReference(
            "intermediary-service",
            "Intermediary",
            "https://registry.example/wrp"
        )), List.of());
        clientService.setIntendedUses(List.of(new IntendedUse(
            "login",
            "2026-01-02",
            null,
            List.of(new MultiLangString("en", "Authenticate the user")),
            List.of(new Policy("PrivacyPolicy", "https://client.example/privacy"))
        )));
        relyingParty.setServices(List.of(intermediaryService, clientService));

        walletRelyingPartyRepository.saveAndFlush(relyingParty);
        var clientServiceId = clientService.getId();
        entityManager.clear();

        WalletRelyingPartyService actualClient = repository.findById(clientServiceId).orElseThrow();
        IntendedUse actualIntendedUse = actualClient.getIntendedUses().getFirst();
        WalletRelyingPartyService actualIntermediary = actualClient.getWalletRelyingParty().getServices()
            .stream()
            .filter(service -> service.getServiceIdentifier().equals("intermediary-service"))
            .findFirst()
            .orElseThrow();

        assertAll(
            () -> assertEquals(List.of(new IntermediaryReference(
                "intermediary-service",
                "Intermediary",
                "https://registry.example/wrp"
            )), actualClient.getUsesIntermediaries()),
            () -> assertTrue(intermediaryFlag(actualIntermediary.getId())),
            () -> assertEquals(List.of("client-service"), actualIntermediary.getServedWRPServices()),
            () -> assertEquals("login", actualIntendedUse.getIntendedUseIdentifier()),
            () -> assertEquals("2026-01-02", actualIntendedUse.getCreatedAt()),
            () -> assertNull(actualIntendedUse.getRevokedAt()),
            () -> assertEquals(List.of(new MultiLangString("en", "Authenticate the user")),
                actualIntendedUse.getPurpose()),
            () -> assertEquals(List.of(new Policy("PrivacyPolicy", "https://client.example/privacy")),
                actualIntendedUse.getPrivacyPolicy()),
            () -> assertEquals(clientServiceId, actualIntendedUse.getWalletRelyingPartyService().getId())
        );
    }

    private boolean intermediaryFlag(java.util.UUID serviceId) {
        return (boolean) entityManager.createNativeQuery(
            "SELECT is_intermediary FROM wallet_relying_party_service WHERE id = :serviceId"
        ).setParameter("serviceId", serviceId.toString()).getSingleResult();
    }

    @Test
    @Transactional
    @DisplayName("rejects invalid intermediary configuration before insert")
    void rejectsInvalidIntermediaryConfigurationBeforeInsert() {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Invalid", relyingParty, List.of());
        ReflectionTestUtils.setField(service, "isIntermediary", true);
        ReflectionTestUtils.setField(service, "usesIntermediaries", List.of(
            new IntermediaryReference("proxy", "Proxy", "https://registry.example/wrp")
        ));
        relyingParty.setServices(List.of(service));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> repository.saveAndFlush(service)
        );
        assertEquals("An intermediary service cannot use intermediaries", exception.getCause().getMessage());
    }

    @Test
    @Transactional
    @DisplayName("rejects invalid intermediary configuration before update")
    void rejectsInvalidIntermediaryConfigurationBeforeUpdate() {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service", relyingParty, List.of());
        relyingParty.setServices(List.of(service));
        repository.saveAndFlush(service);
        entityManager.clear();

        WalletRelyingPartyService managedService = repository.findById(service.getId()).orElseThrow();
        ReflectionTestUtils.setField(managedService, "servedWRPServices", List.of("client-service"));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> repository.flush()
        );
        assertEquals("Only an intermediary service can serve WRP services", exception.getCause().getMessage());
    }

    @Test
    @Transactional
    @DisplayName("rejects invalid intermediary configuration when loading")
    void rejectsInvalidIntermediaryConfigurationWhenLoading() {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service", relyingParty, List.of());
        relyingParty.setServices(List.of(service));
        repository.saveAndFlush(service);
        var serviceId = service.getId();
        entityManager.createNativeQuery(
            "UPDATE wallet_relying_party_service SET served_wrp_services = JSON_ARRAY('client-service') WHERE id = :serviceId"
        ).setParameter("serviceId", serviceId.toString()).executeUpdate();
        entityManager.clear();

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> repository.findById(serviceId)
        );

        assertTrue(hasCauseMessage(exception, "Only an intermediary service can serve WRP services"));
    }

    private boolean hasCauseMessage(Throwable exception, String expectedMessage) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (expectedMessage.equals(cause.getMessage())) {
                return true;
            }
        }
        return false;
    }

    @Test
    @Transactional
    @DisplayName("keeps intended uses when resetting them from the current collection")
    void keepsIntendedUsesWhenResettingFromCurrentCollection() {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service Trade Name", relyingParty, List.of());
        service.setIntendedUses(List.of(new IntendedUse(
            "login",
            "2026-01-02",
            null,
            List.of(),
            List.of()
        )));
        relyingParty.setServices(List.of(service));
        walletRelyingPartyRepository.saveAndFlush(relyingParty);
        var serviceId = service.getId();
        entityManager.clear();

        WalletRelyingPartyService managedService = repository.findById(serviceId).orElseThrow();
        managedService.setIntendedUses(managedService.getIntendedUses());
        repository.flush();
        entityManager.clear();

        WalletRelyingPartyService reloadedService = repository.findById(serviceId).orElseThrow();
        assertEquals(1, reloadedService.getIntendedUses().size());
        assertEquals("login", reloadedService.getIntendedUses().getFirst().getIntendedUseIdentifier());
    }

    @Test
    @Transactional
    @DisplayName("keeps instances when resetting them from the current collection")
    void keepsInstancesWhenResettingThemFromCurrentCollection() {
        var instance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService(
            "Service Trade Name",
            relyingParty,
            List.of(instance)
        );
        relyingParty.setServices(List.of(service));
        walletRelyingPartyRepository.saveAndFlush(relyingParty);
        var serviceId = service.getId();
        var instanceId = instance.getId();
        entityManager.clear();

        WalletRelyingPartyService managedService = repository.findById(serviceId).orElseThrow();
        managedService.setRelyingPartyInstances(managedService.getRelyingPartyInstances());
        repository.flush();
        entityManager.clear();

        WalletRelyingPartyService reloadedService = repository.findById(serviceId).orElseThrow();
        RelyingPartyInstance reloadedInstance = reloadedService.getRelyingPartyInstances().getFirst();
        assertAll(
            () -> assertEquals(instanceId, reloadedInstance.getId()),
            () -> assertEquals(serviceId, reloadedInstance.getWalletRelyingPartyService().getId()),
            () -> assertTrue(instanceRepository.findById(instanceId).isPresent())
        );
    }

    @Test
    @Transactional
    @DisplayName("deleting a service deletes its instances but keeps its relying party")
    void deletingServiceDeletesItsInstancesOnly() {
        var instance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service Trade Name", relyingParty, List.of(instance));
        service.setIntendedUses(List.of(new IntendedUse(
            "delete-test",
            "2026-01-02",
            null,
            List.of(),
            List.of()
        )));
        relyingParty.setServices(List.of(service));

        WalletRelyingPartyService savedService = repository.saveAndFlush(service);
        RelyingPartyInstance savedInstance = instanceRepository
            .findById(savedService.getRelyingPartyInstances().getFirst().getId())
            .orElseThrow();
        WalletRelyingPartyService serviceToDelete = savedInstance.getWalletRelyingPartyService();
        var relyingPartyId = serviceToDelete.getWalletRelyingParty().getId();
        var instanceId = savedInstance.getId();

        serviceToDelete.getWalletRelyingParty().getServices().remove(serviceToDelete);
        repository.delete(serviceToDelete);
        repository.flush();
        entityManager.clear();

        assertAll(
            () -> assertTrue(repository.findById(savedService.getId()).isEmpty()),
            () -> assertTrue(instanceRepository.findById(instanceId).isEmpty()),
            () -> assertEquals(0L, entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM wallet_relying_party_service_intended_use WHERE wallet_relying_party_service_id = :serviceId"
            ).setParameter("serviceId", savedService.getId().toString()).getSingleResult()),
            () -> assertTrue(walletRelyingPartyRepository.findById(relyingPartyId).isPresent()),
            () -> assertTrue(walletRelyingPartyRepository.findById(relyingPartyId).orElseThrow()
                .getServices().isEmpty())
        );
    }
}

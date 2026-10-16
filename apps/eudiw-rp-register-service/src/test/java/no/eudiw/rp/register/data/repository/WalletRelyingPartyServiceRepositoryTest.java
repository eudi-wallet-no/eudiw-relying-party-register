package no.eudiw.rp.register.data.repository;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.domain.Credential;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    void persistsIntermediaryRelationshipsAndIntendedUses() throws Exception {
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
        var loginIntendedUse = new IntendedUse(
            "login",
            "2026-01-02",
            null,
            List.of(new MultiLangString("en", "Authenticate the user")),
            List.of(new Policy("PrivacyPolicy", "https://client.example/privacy"))
        );
        loginIntendedUse.setCredentials(List.of(
            new Credential(
                "dc+sd-jwt",
                meta("vct_values", List.of("urn:example:pid")),
                claims("namespace", null, 0)
            ),
            new Credential(
                "mso_mdoc",
                meta("doctype", "org.iso.18013.5.1.mDL"),
                claims("org.iso.18013.5.1", "given_name")
            )
        ));
        clientService.setIntendedUses(List.of(loginIntendedUse));
        relyingParty.setServices(List.of(intermediaryService, clientService));

        walletRelyingPartyRepository.saveAndFlush(relyingParty);
        var clientServiceId = clientService.getId();
        entityManager.clear();

        WalletRelyingPartyService actualClient = repository.findById(clientServiceId).orElseThrow();
        IntendedUse actualIntendedUse = actualClient.getIntendedUses().getFirst();
        Credential actualSdJwtCredential = actualIntendedUse.getCredentials().stream()
            .filter(credential -> credential.getFormat().equals("dc+sd-jwt"))
            .findFirst()
            .orElseThrow();
        Credential actualMdocCredential = actualIntendedUse.getCredentials().stream()
            .filter(credential -> credential.getFormat().equals("mso_mdoc"))
            .findFirst()
            .orElseThrow();
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
            () -> assertEquals(clientServiceId, actualIntendedUse.getWalletRelyingPartyService().getId()),
            () -> assertEquals("dc+sd-jwt", actualSdJwtCredential.getFormat()),
            () -> assertEquals(meta("vct_values", List.of("urn:example:pid")), actualSdJwtCredential.getMeta()),
            () -> assertEquals(claims("namespace", null, 0), actualSdJwtCredential.getClaims()),
            () -> assertEquals("mso_mdoc", actualMdocCredential.getFormat()),
            () -> assertEquals(meta("doctype", "org.iso.18013.5.1.mDL"), actualMdocCredential.getMeta()),
            () -> assertEquals(claims("org.iso.18013.5.1", "given_name"), actualMdocCredential.getClaims()),
            () -> assertEquals(actualIntendedUse.getId(), actualSdJwtCredential.getIntendedUse().getId())
        );
    }

    @Test
    @Transactional
    @DisplayName("rejects invalid credential claims before insert")
    void rejectsInvalidCredentialBeforeInsert() throws Exception {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service", relyingParty, List.of());
        var intendedUse = new IntendedUse("login", "2026-01-02", null, List.of(), List.of());
        var credential = new Credential(
            "mso_mdoc",
            meta("doctype", "org.iso.18013.5.1.mDL"),
            claims("given_name")
        );
        ReflectionTestUtils.setField(credential, "claims", List.of(Map.of("path", List.of())));
        intendedUse.setCredentials(List.of(credential));
        service.setIntendedUses(List.of(intendedUse));
        relyingParty.setServices(List.of(service));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> repository.saveAndFlush(service)
        );
        assertTrue(hasCauseMessage(exception, "Each credential claim must have a non-empty path array"));
    }

    @Test
    @Transactional
    @DisplayName("rejects invalid credential claims when loading")
    void rejectsInvalidCredentialWhenLoading() {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service", relyingParty, List.of());
        var intendedUse = new IntendedUse("login", "2026-01-02", null, List.of(), List.of());
        var credential = new Credential(
            "mso_mdoc",
            meta("doctype", "org.iso.18013.5.1.mDL"),
            claims("given_name")
        );
        intendedUse.setCredentials(List.of(credential));
        service.setIntendedUses(List.of(intendedUse));
        relyingParty.setServices(List.of(service));
        repository.saveAndFlush(service);

        entityManager.createNativeQuery(
            "UPDATE wallet_relying_party_service_intended_use_credential " +
                "SET claims = JSON_ARRAY(JSON_OBJECT('path', JSON_ARRAY())) WHERE id = :credentialId"
        ).setParameter("credentialId", credential.getId().toString()).executeUpdate();
        entityManager.clear();

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> repository.findById(service.getId())
        );

        assertTrue(hasCauseMessage(exception, "Each credential claim must have a non-empty path array"));
    }

    @Test
    @Transactional
    @DisplayName("keeps credentials when resetting them from the current collection")
    void keepsCredentialsWhenResettingThemFromTheCurrentCollection() {
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service", relyingParty, List.of());
        var intendedUse = new IntendedUse("login", "2026-01-02", null, List.of(), List.of());
        intendedUse.setCredentials(List.of(new Credential(
            "mso_mdoc",
            meta("doctype", "org.iso.18013.5.1.mDL"),
            claims("given_name")
        )));
        service.setIntendedUses(List.of(intendedUse));
        relyingParty.setServices(List.of(service));
        repository.saveAndFlush(service);
        var serviceId = service.getId();
        var credentialId = intendedUse.getCredentials().getFirst().getId();
        entityManager.clear();

        WalletRelyingPartyService managedService = repository.findById(serviceId).orElseThrow();
        IntendedUse managedIntendedUse = managedService.getIntendedUses().getFirst();
        managedIntendedUse.setCredentials(managedIntendedUse.getCredentials());
        repository.flush();
        entityManager.clear();

        WalletRelyingPartyService reloadedService = repository.findById(serviceId).orElseThrow();
        Credential reloadedCredential = reloadedService.getIntendedUses().getFirst().getCredentials().getFirst();
        assertAll(
            () -> assertEquals(credentialId, reloadedCredential.getId()),
            () -> assertEquals("mso_mdoc", reloadedCredential.getFormat()),
            () -> assertTrue(entityManager.find(Credential.class, credentialId) != null)
        );

        reloadedService.getIntendedUses().getFirst().setCredentials(List.of());
        repository.flush();
        entityManager.clear();
        assertNull(entityManager.find(Credential.class, credentialId));
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
        var intendedUse = new IntendedUse(
            "delete-test",
            "2026-01-02",
            null,
            List.of(),
            List.of()
        );
        intendedUse.setCredentials(List.of(new Credential(
            "mso_mdoc",
            meta("doctype", "org.iso.18013.5.1.mDL"),
            claims("given_name")
        )));
        service.setIntendedUses(List.of(intendedUse));
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
            () -> assertEquals(0L, entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM wallet_relying_party_service_intended_use_credential WHERE intended_use_id = :intendedUseId"
            ).setParameter("intendedUseId", intendedUse.getId().toString()).getSingleResult()),
            () -> assertTrue(walletRelyingPartyRepository.findById(relyingPartyId).isPresent()),
            () -> assertTrue(walletRelyingPartyRepository.findById(relyingPartyId).orElseThrow()
                .getServices().isEmpty())
        );
    }

    private static Map<String, Object> meta(String property, Object value) {
        return Map.of(property, value);
    }

    private static List<Map<String, Object>> claims(Object... pathParts) {
        return List.of(Map.of("path", Arrays.asList(pathParts)));
    }
}

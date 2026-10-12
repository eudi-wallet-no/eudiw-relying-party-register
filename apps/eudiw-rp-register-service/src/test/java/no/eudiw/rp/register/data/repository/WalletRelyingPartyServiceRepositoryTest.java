package no.eudiw.rp.register.data.repository;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
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
        var instance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service Trade Name", relyingParty, List.of(instance));
        relyingParty.setServices(List.of(service));

        WalletRelyingPartyService savedService = repository.saveAndFlush(service);
        entityManager.clear();

        WalletRelyingPartyService actual = repository.findById(savedService.getId()).orElseThrow();
        RelyingPartyInstance actualInstance = actual.getRelyingPartyInstances().getFirst();

        assertAll(
            () -> assertEquals("Service Trade Name", actual.getServiceTradeName()),
            () -> assertEquals(relyingParty.getId(), actual.getWalletRelyingParty().getId()),
            () -> assertEquals("Legal Name", actual.getWalletRelyingParty().getLegalName()),
            () -> assertEquals(1, actual.getRelyingPartyInstances().size()),
            () -> assertEquals(actual.getId(), actualInstance.getWalletRelyingPartyService().getId()),
            () -> assertTrue(actual.getCreatedMs() > 0),
            () -> assertTrue(actual.getLastUpdatedMs() > 0)
        );
    }

    @Test
    @Transactional
    @DisplayName("deleting a service deletes its instances but keeps its relying party")
    void deletingServiceDeletesItsInstancesOnly() {
        var instance = new RelyingPartyInstance(List.of(), List.of(), List.of());
        var relyingParty = new WalletRelyingParty("Legal Name", generateValidOrgno(), true, List.of());
        var service = new WalletRelyingPartyService("Service Trade Name", relyingParty, List.of(instance));
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
            () -> assertTrue(walletRelyingPartyRepository.findById(relyingPartyId).isPresent()),
            () -> assertTrue(walletRelyingPartyRepository.findById(relyingPartyId).orElseThrow()
                .getServices().isEmpty())
        );
    }
}

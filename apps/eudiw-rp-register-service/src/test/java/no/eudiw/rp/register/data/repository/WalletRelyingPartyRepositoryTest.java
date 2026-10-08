package no.eudiw.rp.register.data.repository;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static no.eudiw.rp.register.testdata.TestDataGenerator.generateValidOrgno;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When using WalletRelyingPartyRepository")
@ActiveProfiles("junit")
class WalletRelyingPartyRepositoryTest {

    @Autowired
    private WalletRelyingPartyRepository repository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("persists the relying party and its related services and instances")
    void persistsRelyingPartyGraph() {
        WalletRelyingParty relyingParty = EntityGenerator.generateWalletRelyingParty(2);

        WalletRelyingParty savedRelyingParty = repository.saveAndFlush(relyingParty);
        entityManager.clear();

        WalletRelyingParty actual = repository.findById(savedRelyingParty.getId()).orElseThrow();

        assertAll(
            () -> assertEquals(relyingParty.getLegalName(), actual.getLegalName()),
            () -> assertEquals(relyingParty.getOrgno(), actual.getOrgno()),
            () -> assertEquals(relyingParty.isPsb(), actual.isPsb()),
            () -> assertEquals(2, actual.getServices().size()),
            () -> assertTrue(actual.getCreatedMs() > 0),
            () -> assertTrue(actual.getLastUpdatedMs() > 0)
        );

        for (int index = 0; index < actual.getServices().size(); index++) {
            WalletRelyingPartyService expectedService = relyingParty.getServices().get(index);
            WalletRelyingPartyService actualService = actual.getServices().get(index);

            assertAll(
                () -> assertEquals(expectedService.getServiceTradeName(), actualService.getServiceTradeName()),
                () -> assertEquals(actual.getId(), actualService.getWalletRelyingParty().getId()),
                () -> assertEquals(1, actualService.getRelyingPartyInstances().size()),
                () -> assertEquals(actualService.getId(),
                    actualService.getRelyingPartyInstances().getFirst().getWalletRelyingPartyService().getId())
            );
        }
    }

    @Test
    @DisplayName("finds a relying party by its organization number")
    void findsByOrgno() {
        WalletRelyingParty relyingParty = EntityGenerator.generateWalletRelyingParty(0);
        repository.saveAndFlush(relyingParty);

        WalletRelyingParty actual = repository.findByOrgno(relyingParty.getOrgno()).orElseThrow();

        assertEquals(relyingParty.getId(), actual.getId());
        assertTrue(repository.existsByOrgno(relyingParty.getOrgno()));
    }

    @Test
    @DisplayName("returns no relying party for an unknown organization number")
    void doesNotFindUnknownOrgno() {
        String unknownOrgno = generateValidOrgno();

        assertTrue(repository.findByOrgno(unknownOrgno).isEmpty());
        assertFalse(repository.existsByOrgno(unknownOrgno));
    }
}

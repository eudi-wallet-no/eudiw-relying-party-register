package no.eudiw.rp.register.data.repository;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.domain.Identifier;
import no.eudiw.rp.register.domain.Law;
import no.eudiw.rp.register.domain.LegalPerson;
import no.eudiw.rp.register.domain.Policy;
import no.eudiw.rp.register.domain.SupervisoryAuthority;
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

import java.util.List;

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

        for (WalletRelyingPartyService expectedService : relyingParty.getServices()) {
            WalletRelyingPartyService actualService = actual.getServices().stream()
                .filter(service -> service.getId().equals(expectedService.getId()))
                .findFirst()
                .orElseThrow();

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
    @DisplayName("persists TS5 WalletRelyingParty data")
    void persistsTs5Data() {
        WalletRelyingParty relyingParty = EntityGenerator.generateWalletRelyingParty(0);
        relyingParty.setTradeName("Trading name");
        relyingParty.setCountry("NO");
        relyingParty.setIdentifier(List.of(new Identifier("urn:test:orgno", "123456785")));
        relyingParty.setPolicy(List.of(new Policy("urn:test:privacy", "https://example.org/privacy")));
        relyingParty.setX5c(List.of("certificate-chain"));
        relyingParty.setPostalAddress(List.of("Example street 1"));
        relyingParty.setEmail(List.of("contact@example.org"));
        relyingParty.setPhone(List.of("+4712345678"));
        relyingParty.setInfoURI(List.of("https://example.org/info"));
        relyingParty.setLegalPerson(new LegalPerson(
            List.of(new Law("https://example.org/law", "Section 1"))));
        relyingParty.setSupervisoryAuthority(new SupervisoryAuthority(
            "Data Protection Authority",
            "NO",
            List.of("dpa@example.org"),
            List.of("+4798765432"),
            List.of("https://example.org/report")));

        WalletRelyingParty saved = repository.saveAndFlush(relyingParty);
        entityManager.clear();

        WalletRelyingParty actual = repository.findById(saved.getId()).orElseThrow();
        assertAll(
            () -> assertEquals("Trading name", actual.getTradeName()),
            () -> assertEquals("NO", actual.getCountry()),
            () -> assertEquals("WalletRelyingParty", actual.getProviderType()),
            () -> assertEquals(new Identifier("urn:test:orgno", "123456785").getType(),
                actual.getIdentifier().getFirst().getType()),
            () -> assertEquals("https://example.org/privacy", actual.getPolicy().getFirst().getPolicyURI()),
            () -> assertEquals(List.of("certificate-chain"), actual.getX5c()),
            () -> assertEquals(List.of("Example street 1"), actual.getPostalAddress()),
            () -> assertEquals(List.of("contact@example.org"), actual.getEmail()),
            () -> assertEquals(List.of("+4712345678"), actual.getPhone()),
            () -> assertEquals(List.of("https://example.org/info"), actual.getInfoURI()),
            () -> assertEquals("https://example.org/law",
                actual.getLegalPerson().getEstablishedByLaw().getFirst().getLegislativeIdentifier()),
            () -> assertEquals("Data Protection Authority", actual.getSupervisoryAuthority().getName()),
            () -> assertEquals(List.of("dpa@example.org"), actual.getSupervisoryAuthority().getEmail()),
            () -> assertEquals(List.of("+4798765432"), actual.getSupervisoryAuthority().getPhone()),
            () -> assertEquals(List.of("https://example.org/report"),
                actual.getSupervisoryAuthority().getFormURI())
        );
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

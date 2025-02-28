package no.eudiw.rp.register.data.repository;

import jakarta.annotation.Resource;
import no.eudiw.rp.register.TestDataGenerator;
import no.eudiw.rp.register.data.entity.RelyingParty;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RelyingPartyRepositoryTest {

    @Resource
    private RelyingPartyRepository rpRepository;

    @AfterEach
    void clearRepositoryAfterTests() {
        rpRepository.deleteAll();
    }

    @Nested
    @DisplayName("When READING records from the RelyingPartyRepository ...")
    class ReadTests {

        @Test
        @DisplayName("then the read goes well if a record with the orgno exists")
        void testFindByOrgno() {
            RelyingParty testRelyingParty =
                rpRepository.save(TestDataGenerator.generateRelyingPartyNoId());

            String orgno = testRelyingParty.getOrgno();
            RelyingParty rpOut = rpRepository.findByOrgno(orgno).orElse(null);

            assertNotNull(rpOut);
            assertAll(
                () -> assertEquals(testRelyingParty.getOrgno(), rpOut.getOrgno()),
                () -> assertEquals(testRelyingParty.getName(), rpOut.getName()),
                () -> assertEquals(testRelyingParty.getPublicSector(), rpOut.getPublicSector())
            );
        }

        @Test
        @DisplayName("then nothing is read if the orgno is not in the RelyingPartyRepository")
        void testFindNonexistentOrgno() {
            String nonExistentOrgno = TestDataGenerator.generateOrgno();
            assertTrue(rpRepository.findByOrgno(nonExistentOrgno).isEmpty());
        }
    }

    @Nested
    @DisplayName("When CREATING records in the repository ...")
    class CreateTests {

        @Test
        @DisplayName("then creation goes well if ID field is null and orgno does not exist")
        void testCreateNewRelyingParty() {
            RelyingParty testRelyingParty =
                TestDataGenerator.generateRelyingPartyNoId();
            String orgno = testRelyingParty.getOrgno();

            rpRepository.save(testRelyingParty);
            RelyingParty rpOut = rpRepository.findByOrgno(orgno).orElse(null);
            assertNotNull(rpOut);

            assertAll(
                () -> assertEquals(testRelyingParty.getOrgno(), rpOut.getOrgno()),
                () -> assertEquals(testRelyingParty.getName(), rpOut.getName()),
                () -> assertEquals(testRelyingParty.getPublicSector(), rpOut.getPublicSector())
            );
        }

        @Test
        @DisplayName("then creation is rejected if orgno already exists")
        void testCreationRejectedWhenOrgnoAlreadyExists() {
            RelyingParty testRelyingParty =
                rpRepository.save(TestDataGenerator.generateRelyingPartyNoId());

            String existingOrgno = testRelyingParty.getOrgno();

            RelyingParty newRelyingPartyWithExistingOrgno =
                TestDataGenerator.generateRelyingPartyNoId();
            newRelyingPartyWithExistingOrgno.setOrgno(existingOrgno);

            // assert second creation with same orgno is rejected.
            assertThrows(
                DataIntegrityViolationException.class,
                () -> rpRepository.saveAndFlush(newRelyingPartyWithExistingOrgno)
            );
        }
    }

    @Nested
    @DisplayName("When UPDATING records in the RelyingPartyRepository ...")
    class UpdateTests {

        @Test
        @DisplayName("then the update goes well if the ID already exists")
        void testUpdateOrgnoForExistingRelyingParty() {
            RelyingParty testRelyingParty =
                rpRepository.save(TestDataGenerator.generateRelyingPartyNoId());

            String orgno = testRelyingParty.getOrgno();

            // create distinct orgno from existing.
            char[] orgnoChars = orgno.toCharArray();
            orgnoChars[0] ^= 1;
            String newOrgno = String.valueOf(orgnoChars);

            testRelyingParty.setOrgno(newOrgno);
            rpRepository.save(testRelyingParty);

            RelyingParty rpModifiedOut =
                rpRepository.findById(testRelyingParty.getId()).orElse(null);

            assertNotNull(rpModifiedOut);
            assertAll(
                // assert update successful, and that no record exists for old orgno.
                () -> assertEquals(newOrgno, rpModifiedOut.getOrgno()),
                () -> assertTrue(rpRepository.findByOrgno(orgno).isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("When DELETING records from the RelyingPartyRepository ...")
    class DeleteTests {

        @Test
        @DisplayName("then deletion goes well if the entity is in the repository")
        void testDeleteExistingOrgno() {
            RelyingParty testRelyingParty =
                rpRepository.save(TestDataGenerator.generateRelyingPartyNoId());

            RelyingParty testRelyingPartyOut =
                rpRepository.findByOrgno(testRelyingParty.getOrgno()).orElse(null);
            assertNotNull(testRelyingPartyOut);
            rpRepository.delete(testRelyingPartyOut);

            assertAll(
                () -> assertTrue(rpRepository.findByOrgno(testRelyingPartyOut.getOrgno()).isEmpty()),
                () -> assertTrue(rpRepository.findById(testRelyingPartyOut.getId()).isEmpty())
            );
        }
    }
}

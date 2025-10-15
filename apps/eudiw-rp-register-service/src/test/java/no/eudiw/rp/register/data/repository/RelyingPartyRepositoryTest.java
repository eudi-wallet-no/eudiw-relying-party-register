package no.eudiw.rp.register.data.repository;

import jakarta.annotation.Resource;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.data.entity.RelyingParty;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RelyingPartyRepositoryTest {

    @Resource
    private RelyingPartyRepository rpRepository;

    @BeforeEach
    void clearRepositoryBeforeTests() {
        rpRepository.deleteAll();
    }

    @Nested
    @DisplayName("When READING records from the RelyingPartyRepository ...")
    class ReadTests {

        @Test
        @DisplayName("then the read goes well if a record with the orgno exists")
        void testFindByOrgno() {
            RelyingParty testRelyingParty =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            String orgno = testRelyingParty.getOrgno();
            RelyingParty rpOut = rpRepository.findByOrgno(orgno).getFirst();

            assertAll(
                () -> assertEquals(testRelyingParty.getOrgno(), rpOut.getOrgno()),
                () -> assertEquals(testRelyingParty.getName(), rpOut.getName()),
                () -> assertEquals(testRelyingParty.getPublicSector(), rpOut.getPublicSector())
            );
        }

        @Test
        @DisplayName("then nothing is read if the orgno is not in the RelyingPartyRepository")
        void testFindNonexistentOrgno() {
            String nonExistentOrgno = TestDataGenerator.generateValidOrgno();
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
                EntityGenerator.generateRelyingPartyNoId();
            String orgno = testRelyingParty.getOrgno();

            rpRepository.save(testRelyingParty);
            RelyingParty rpOut = rpRepository.findByOrgno(orgno).getFirst();

            assertAll(
                () -> assertEquals(testRelyingParty.getOrgno(), rpOut.getOrgno()),
                () -> assertEquals(testRelyingParty.getName(), rpOut.getName()),
                () -> assertEquals(testRelyingParty.getPublicSector(), rpOut.getPublicSector()),
                // ID must be valid UUID if non-null; else UUID constructor would have failed.
                () -> assertNotNull(rpOut.getId())
            );
        }

        @Test
        @DisplayName("then creation of duplicate orgno is accepted")
        void testCreationAcceptedWhenOrgnoAlreadyExists() {
            RelyingParty testRelyingParty =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            String existingOrgno = testRelyingParty.getOrgno();

            RelyingParty newRelyingPartyWithExistingOrgno =
                EntityGenerator.generateRelyingPartyNoId();
            newRelyingPartyWithExistingOrgno.setOrgno(existingOrgno);

            rpRepository.saveAndFlush(newRelyingPartyWithExistingOrgno);

            List<RelyingParty> rps = rpRepository.findByOrgno(existingOrgno);
            assertTrue(rps.containsAll(List.of(testRelyingParty, newRelyingPartyWithExistingOrgno)));
        }

        @Test
        @DisplayName("then the relying party is initialized with proper timestamps")
        void testCreationHasProperTimestamps() {
            long tInit = Instant.now().toEpochMilli();

            RelyingParty testRelyingParty =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            RelyingParty relyingPartyOut =
                rpRepository.findByOrgno(testRelyingParty.getOrgno()).getFirst();

            assertAll(
                () -> assertTrue(tInit <= relyingPartyOut.getCreatedMs()),
                () -> assertEquals(relyingPartyOut.getCreatedMs(), relyingPartyOut.getLastUpdatedMs())
            );
        }

        @Test
        @DisplayName("then two distinct creations will produce two distinct IDs")
        void testCreationAssignsDistinctIds() {

            RelyingParty testRelyingParty1 =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            RelyingParty testRelyingParty2 =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            UUID testRelyingPartyIdOut1 =
                rpRepository.findByOrgno(testRelyingParty1.getOrgno())
                            .getFirst()
                            .getId();

            UUID testRelyingPartyIdOut2 =
                rpRepository.findByOrgno(testRelyingParty2.getOrgno())
                            .getFirst()
                            .getId();

            assertAll(
                () -> assertNotNull(testRelyingPartyIdOut1),
                () -> assertNotNull(testRelyingPartyIdOut2),
                () -> assertNotEquals(testRelyingPartyIdOut1, testRelyingPartyIdOut2)
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
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            String oldOrgno = testRelyingParty.getOrgno();
            String newOrgno = TestDataGenerator.generateValidOrgno();

            testRelyingParty.setOrgno(newOrgno);
            rpRepository.save(testRelyingParty);

            RelyingParty rpModifiedOut =
                rpRepository.findById(testRelyingParty.getId()).orElse(null);

            assertNotNull(rpModifiedOut);
            assertAll(
                () -> assertEquals(newOrgno, rpModifiedOut.getOrgno()),
                () -> assertTrue(rpRepository.findByOrgno(oldOrgno).isEmpty())
            );
        }

        @Test
        @DisplayName("then the update preserves the ID of the updated relying party")
        void testUpdateRespectsIds() {
            RelyingParty testRelyingParty =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            UUID testRelyingPartyIdOut1 =
                rpRepository.findByOrgno(testRelyingParty.getOrgno())
                            .getFirst()
                            .getId();

            testRelyingParty.setName(EntityGenerator.generateName());
            rpRepository.save(testRelyingParty);

            UUID testRelyingPartyIdOut2 =
                rpRepository.findByOrgno(testRelyingParty.getOrgno())
                            .getFirst()
                            .getId();

            assertAll(
                () -> assertNotNull(testRelyingPartyIdOut1),
                () -> assertNotNull(testRelyingPartyIdOut2),
                () -> assertEquals(testRelyingPartyIdOut1, testRelyingPartyIdOut2)
            );
        }

        @Test
        @DisplayName("then the update respects creation time but updates last-updated time")
        void testUpdateRespectsTimestamps() {
            long tInit = Instant.now().toEpochMilli();

            RelyingParty testRelyingParty =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            RelyingParty relyingPartyOut1 =
                rpRepository.findByOrgno(testRelyingParty.getOrgno()).getFirst();

            // query an update, which should update lastUpdatedMs but not touch createdMs.
            relyingPartyOut1.setName(EntityGenerator.generateName());
            rpRepository.save(relyingPartyOut1);

            RelyingParty relyingPartyOut2 =
                rpRepository.findByOrgno(testRelyingParty.getOrgno()).getFirst();

            long tCreated1 = relyingPartyOut1.getCreatedMs();
            long tLastUpdated1 = relyingPartyOut1.getLastUpdatedMs();
            long tCreated2 = relyingPartyOut2.getCreatedMs();
            long tLastUpdated2 = relyingPartyOut2.getLastUpdatedMs();

            assertAll(
                () -> assertTrue(tInit <= tCreated1),
                () -> assertEquals(tCreated1, tLastUpdated1),
                () -> assertEquals(tCreated1, tCreated2),
                () -> assertTrue(tCreated2 <= tLastUpdated2)
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
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            RelyingParty testRelyingPartyOut =
                rpRepository.findByOrgno(testRelyingParty.getOrgno()).getFirst();
            rpRepository.delete(testRelyingPartyOut);

            assertAll(
                () -> assertTrue(rpRepository.findByOrgno(testRelyingPartyOut.getOrgno()).isEmpty()),
                () -> assertTrue(rpRepository.findById(testRelyingPartyOut.getId()).isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("When searching ...")
    class SearchTests {

        @Test
        @DisplayName("then deleted RPs are not included in regular searches")
        void testSearchDoesNotIncludeDeleted() {
            RelyingParty testRelyingParty =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());

            testRelyingParty.setDeleted(true);
            rpRepository.save(testRelyingParty);

            List<RelyingParty> relyingParties =
                rpRepository.searchQuery(testRelyingParty.getName(), true);
            assertNotNull(relyingParties);
            assertTrue(relyingParties.isEmpty());
        }

        @Test
        @DisplayName("advanced search with pagination")
        void testAdvancedSearchPagination() {
            RelyingParty testRelyingParty = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            rpRepository.save(testRelyingParty);

            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
            Page<RelyingParty> relyingParties = rpRepository.searchQueryPaged(
                "",
                false,
                List.of(),
                pageable
            );

            assertNotNull(relyingParties);
            assertTrue(relyingParties.hasContent());
            assertEquals(1, relyingParties.getTotalElements());
            RelyingParty relyingParty = relyingParties.getContent().getFirst();
            assertNotNull(relyingParty);
            assertEquals(testRelyingParty.getOrgno(), relyingParty.getOrgno());
        }

        @Test
        @DisplayName("advanced search with pagination and filter entitlements")
        void testAdvancedSearchPaginationFilterEntitlements() {
            RelyingParty testRelyingParty = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            rpRepository.save(testRelyingParty);

            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
            Page<RelyingParty> relyingParties = rpRepository.searchQueryPaged(
                "",
                false,
                List.of("https://uri.etsi.org/19475/Entitlement/Service_Provider", "https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
                pageable
            );

            assertNotNull(relyingParties);
            assertTrue(relyingParties.hasContent());
            assertEquals(1, relyingParties.getTotalElements());
            RelyingParty relyingParty = relyingParties.getContent().getFirst();
            assertNotNull(relyingParty);
            assertEquals(testRelyingParty.getOrgno(), relyingParty.getOrgno());
        }

        @Test
        @DisplayName("advanced search with pagination and filter entitlements out")
        void testAdvancedSearchPaginationFilterEntitlementsOut() {
            RelyingParty testRelyingParty = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            rpRepository.save(testRelyingParty);

            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
            Page<RelyingParty> relyingParties = rpRepository.searchQueryPaged(
                "",
                false,
                List.of("NOT_AN_ENTITLEMENT"),
                pageable
            );

            assertNotNull(relyingParties);
            assertFalse(relyingParties.hasContent());
        }

        @Test
        @DisplayName("advanced search with pagination and filter entitlements multiple")
        void testAdvancedSearchPaginationFilterEntitlementsMultiple() {
            RelyingParty testRelyingParty1 = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            rpRepository.save(testRelyingParty1);

            RelyingParty testRelyingParty2 = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            rpRepository.save(testRelyingParty2);

            RelyingParty testRelyingParty3 = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            testRelyingParty3.setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("NOT_AN_ENTITLEMENT")));
            rpRepository.save(testRelyingParty3);

            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
            Page<RelyingParty> relyingParties = rpRepository.searchQueryPaged(
                "",
                false,
                List.of("https://uri.etsi.org/19475/Entitlement/Service_Provider", "https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
                pageable
            );

            assertNotNull(relyingParties);
            assertTrue(relyingParties.hasContent());
            assertEquals(3, relyingParties.getTotalElements());
            RelyingParty relyingParty = relyingParties.getContent().getFirst();
            assertNotNull(relyingParty);

        }

        @Test
        @DisplayName("advanced search with pagination and filter entitlements and name")
        void testAdvancedSearchPaginationFilterEntitlementsAndName() {
            RelyingParty testRelyingParty1 = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            rpRepository.save(testRelyingParty1);

            RelyingParty testRelyingParty2 = rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            rpRepository.save(testRelyingParty2);

            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
            Page<RelyingParty> relyingParties = rpRepository.searchQueryPaged(
                testRelyingParty1.getName(),
                false,
                List.of("https://uri.etsi.org/19475/Entitlement/Service_Provider", "https://uri.etsi.org/19475/Entitlement/QEAA_Provider"),
                pageable
            );

            assertNotNull(relyingParties);
            assertTrue(relyingParties.hasContent());
            assertEquals(1, relyingParties.getTotalElements());
            RelyingParty relyingParty = relyingParties.getContent().getFirst();
            assertNotNull(relyingParty);
            assertEquals(testRelyingParty1.getOrgno(), relyingParty.getOrgno());
        }

        @Test
        @DisplayName("advanced search with pagination page count")
        void testAdvancedSearchPaginationPageCount() {
            for(int i = 0; i < 30; i++) {
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId());
            }

            Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "name"));
            Page<RelyingParty> relyingParties = rpRepository.searchQueryPaged(
                "",
                false,
                List.of(),
                pageable
            );

            assertNotNull(relyingParties);
            assertTrue(relyingParties.hasContent());
            assertEquals(30, relyingParties.getTotalElements());
            assertEquals(30/5, relyingParties.getTotalPages());
            assertEquals(5, relyingParties.getContent().size());
        }
    }
}

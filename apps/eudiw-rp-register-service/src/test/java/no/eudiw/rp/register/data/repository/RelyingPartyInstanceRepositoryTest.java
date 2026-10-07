package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.domain.BaseEntity;
import no.eudiw.rp.register.domain.LegalEntity;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.LegalEntityRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When using the RelyingPartyInstanceRepository")
@ActiveProfiles("junit")
public class RelyingPartyInstanceRepositoryTest {

    @Autowired
    private RelyingPartyInstanceRepository repository;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    private static final Random rng = new Random();

    // generates "realistic" data, with 1..4 instances per legal entity,
    // roughly 20% inactive RPs, and 10% synthetic legal entities.
    private static List<LegalEntity> generateRealisticTestData(int n) {
        var legalEntities = EntityGenerator.generateLegalEntities(n);
        List<RelyingPartyInstance> relyingPartyInstances =
            legalEntities.stream().flatMap(le -> le.getRelyingPartyInstances().stream()).toList();

        relyingPartyInstances.forEach(rpi -> {
            if (rng.nextFloat() >= 0.8) {
                rpi.setActive(false);
            }
        });
        legalEntities.forEach(le -> {
            if (rng.nextFloat() >= 0.9) {
                String syntheticOrgno =
                    le.getOrgno()
                      .replaceFirst("\\d", rng.nextBoolean() ? "2" : "3");
                le.setOrgno(syntheticOrgno);
            }
        });

        return legalEntities;
    }

    @BeforeAll
    static void initSearchTestData(
        @Autowired LegalEntityRepository legalEntityRepository,
        @Autowired RelyingPartyInstanceRepository relyingPartyInstanceRepository) {
        legalEntityRepository.deleteAll();

        int numTestLegalEntities = 50;
        List<LegalEntity> testLegalEntities = generateRealisticTestData(numTestLegalEntities);
        legalEntityRepository.saveAllAndFlush(testLegalEntities);

        assertTrue(relyingPartyInstanceRepository.count() >= numTestLegalEntities);
    }

    @Nested
    @DisplayName("when using the custom RP instance search query")
    class SearchQueryTests {

        @Test
        @DisplayName("then search by trade name uses substring matching")
        void testSearchByTradeNameUsesSubstringMatching() {
            RelyingPartyInstance relyingPartyInstance = EntityGenerator.generateRelyingParty();
            String searchTerm = relyingPartyInstance.getTradeName();

            relyingPartyInstance.setTradeName(TestDataGenerator.generateName() + searchTerm + TestDataGenerator.generateName());
            repository.saveAndFlush(relyingPartyInstance);

            List<RelyingPartyInstance> searchResult = repository.searchRelyingPartyInstances(
                searchTerm,
                List.of(),
                true,
                false,
                Pageable.unpaged()
            ).getContent();

            assertTrue(searchResult.contains(relyingPartyInstance));
        }

        @Test
        @DisplayName("then search by legal entity name uses substring matching")
        void testSearchByLegalEntityNameUsesSubstringMatching() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            String searchTerm = legalEntity.getName();

            legalEntity.setName(TestDataGenerator.generateName() + searchTerm + TestDataGenerator.generateName());
            legalEntityRepository.saveAndFlush(legalEntity);

            Set<UUID> searchResult =
                repository
                    .searchRelyingPartyInstances(
                        searchTerm,
                        List.of(),
                        true,
                        false,
                        Pageable.unpaged())
                    .getContent()
                    .stream()
                    .map(BaseEntity::getId)
                    .collect(Collectors.toSet());

            Set<UUID> expectedResult =
                legalEntity.getRelyingPartyInstances()
                           .stream()
                           .map(BaseEntity::getId)
                           .collect(Collectors.toSet());

            assertEquals(expectedResult, searchResult);
        }

        @Test
        @DisplayName("then search by orgno uses PREFIX matching, and substring search gives no results")
        void testSearchByLegalEntityOrgnoUsesPrefixMatching() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntityRepository.saveAndFlush(legalEntity);

            String orgnoPrefixSearchTerm = legalEntity.getOrgno().substring(0, 8);

            Set<UUID> searchResultByOrgnoPrefix =
                repository
                    .searchRelyingPartyInstances(
                        orgnoPrefixSearchTerm,
                        List.of(),
                        true,
                        false,
                        Pageable.unpaged())
                    .getContent()
                    .stream()
                    .map(BaseEntity::getId)
                    .collect(Collectors.toSet());

            Set<UUID> expectedSearchResultByOrgnoPrefix =
                legalEntity.getRelyingPartyInstances()
                           .stream()
                           .map(BaseEntity::getId)
                           .collect(Collectors.toSet());
            assertEquals(expectedSearchResultByOrgnoPrefix, searchResultByOrgnoPrefix);

            String orgnoSubstringSearchTerm = legalEntity.getOrgno().substring(1, 9);

            List<RelyingPartyInstance> searchResultByOrgnoSubstring =
                repository
                    .searchRelyingPartyInstances(
                        orgnoSubstringSearchTerm,
                        List.of(),
                        true,
                        false,
                        Pageable.unpaged())
                    .getContent();

            assertTrue(searchResultByOrgnoSubstring.isEmpty());
        }

        @Test
        @DisplayName("using a search term which exists in both trade names, orgnos, and legal entity names")
        void testSearchTermInMultipleDifferentFields() {

            RelyingPartyInstance rpi1 = EntityGenerator.generateRelyingParty();
            RelyingPartyInstance rpi2 = EntityGenerator.generateRelyingParty();
            LegalEntity legalEntity = legalEntityRepository.findAll().getFirst();

            String searchTerm = legalEntity.getOrgno();
            rpi1.setTradeName(searchTerm + rpi1.getTradeName());
            rpi2.setTradeName(rpi2.getTradeName() + searchTerm);

            repository.saveAndFlush(rpi1);
            repository.saveAndFlush(rpi2);

            Set<UUID> searchResult =
                repository.searchRelyingPartyInstances(
                              searchTerm,
                              List.of(),
                              true,
                              false,
                              Pageable.unpaged())
                          .getContent()
                          .stream()
                          .map(BaseEntity::getId)
                          .collect(Collectors.toSet());

            Set<UUID> expectedResult =
                legalEntity.getRelyingPartyInstances()
                           .stream()
                           .map(BaseEntity::getId)
                           .collect(Collectors.toSet());
            expectedResult.add(rpi1.getId());
            expectedResult.add(rpi2.getId());

            assertEquals(expectedResult, searchResult);
        }

        @Test
        @DisplayName("all instances returned when no filter applied")
        void testAllInstancesReturnedWhenNoFiltersApplied() {
            List<String> requiredEntitlements = List.of();
            boolean includeInactive = true;
            boolean hideSyntheticOrgnos = false;

            List<RelyingPartyInstance> searchResult = repository.searchRelyingPartyInstances(
                "",
                requiredEntitlements,
                includeInactive,
                hideSyntheticOrgnos,
                Pageable.unpaged()
            ).getContent();

            Set<UUID> searchResultIds = searchResult.stream().map(BaseEntity::getId).collect(Collectors.toSet());
            Set<UUID> allIds = repository.findAll().stream().map(BaseEntity::getId).collect(Collectors.toSet());

            assertEquals(allIds, searchResultIds);
        }

        @Test
        @DisplayName("only active instances returned when includeInactive=false")
        void testOnlyActiveSearchResultsWhenIncludeInactiveFalse() {
            boolean includeInactiveFalse = false;
            List<RelyingPartyInstance> searchResult = repository.searchRelyingPartyInstances(
                "",
                List.of(),
                includeInactiveFalse,
                false,
                Pageable.unpaged()
            ).getContent();

            assertTrue(searchResult.stream().allMatch(RelyingPartyInstance::isActive));
        }

        @Test
        @DisplayName("only instances with required entitlements are returned when entitlements non-empty")
        void testOnlyInstancesWithAllRequiredEntitlementsReturned() {
            Set<String> requiredEntitlements = new HashSet<>(TestDataGenerator.sampleEntitlements(2));
            List<RelyingPartyInstance> searchResult = repository.searchRelyingPartyInstances(
                "",
                requiredEntitlements,
                true,
                false,
                Pageable.unpaged()
            ).getContent();

            searchResult.forEach(rp -> {
                Set<String> relyingPartyEntitlements =
                    rp.getRelyingPartyEntitlements()
                      .stream()
                      .map(RelyingPartyEntitlement::getEntitlement)
                      .collect(Collectors.toSet());
                assertTrue(relyingPartyEntitlements.containsAll(requiredEntitlements));
            });
        }

        @Test
        @DisplayName("only instances with non-synthetic orgno returned when hideSyntheticOrgnos=true")
        void testOnlyInstancesWithNonsyntheticOrgnoReturnedWhenHideSyntheticOrgnosTrue() {
            boolean hideSyntheticOrgnosTrue = true;
            List<RelyingPartyInstance> searchResult = repository.searchRelyingPartyInstances(
                "",
                List.of(),
                true,
                hideSyntheticOrgnosTrue,
                Pageable.unpaged()
            ).getContent();

            Predicate<String> isNonsyntheticOrgno = orgno -> orgno.startsWith("8") || orgno.startsWith("9");

            assertTrue(searchResult.stream()
                                   .map(rp -> rp.getLegalEntity().getOrgno())
                                   .allMatch(isNonsyntheticOrgno));
        }
    }

    @Nested
    @DisplayName("when using JPA sorting")
    class SearchQueryWithOrderingTests {
        private static <T> boolean isSortedBy(List<T> lst, Comparator<T> comparator) {
            for (int i = 0; i < lst.size() - 1; i++) {
                if (comparator.compare(lst.get(i), lst.get(i + 1)) > 0) {
                    return false;
                }
            }
            return true;
        }

        @Test
        @DisplayName("then trade name sorting orders relying parties by trade name")
        void testSearchWithOrderingByTradename() {
            Sort orderingByTradename = Sort.by("tradeName");
            List<RelyingPartyInstance> searchResultByTradename =
                repository.searchRelyingPartyInstances(
                    "",
                    List.of(),
                    true,
                    false,
                    PageRequest.of(0, Integer.MAX_VALUE, orderingByTradename)
                ).getContent();

            assertTrue(isSortedBy(searchResultByTradename,
                                  Comparator.comparing(RelyingPartyInstance::getTradeName)));
        }

        @Test
        @DisplayName("then organization number sorting orders relying parties by organization number")
        void testSearchWithOrderingByOrgno() {
            Sort ordering = Sort.by("legalEntity.orgno");
            List<RelyingPartyInstance> searchResultByOrgno =
                repository.searchRelyingPartyInstances(
                    "",
                    List.of(),
                    true,
                    false,
                    PageRequest.of(0, Integer.MAX_VALUE, ordering)
                ).getContent();

            assertTrue(isSortedBy(searchResultByOrgno,
                                  Comparator.comparing(rp -> rp.getLegalEntity().getOrgno())));
        }

        @Test
        @DisplayName("then creation date sorting orders relying parties by createdMs")
        void testSearchWithOrderingByCreatedMs() {
            Sort ordering = Sort.by("createdMs");
            List<RelyingPartyInstance> searchResultByCreatedMs =
                repository.searchRelyingPartyInstances(
                    "",
                    List.of(),
                    true,
                    false,
                    PageRequest.of(0, Integer.MAX_VALUE, ordering)
                ).getContent();

            assertTrue(isSortedBy(searchResultByCreatedMs,
                                  Comparator.comparing(RelyingPartyInstance::getCreatedMs)));
        }

        @Test
        @DisplayName("then last updated sorting orders relying parties by lastUpdatedMs")
        void testSearchWithOrderingByLastUpdatedMs() {
            Sort ordering = Sort.by("lastUpdatedMs");
            List<RelyingPartyInstance> searchResultByLastUpdatedMs =
                repository.searchRelyingPartyInstances(
                    "",
                    List.of(),
                    true,
                    false,
                    PageRequest.of(0, Integer.MAX_VALUE, ordering)
                ).getContent();

            assertTrue(isSortedBy(searchResultByLastUpdatedMs,
                                  Comparator.comparing(RelyingPartyInstance::getLastUpdatedMs)));
        }

        @Test
        @DisplayName("then an unsorted page request returns every relying party")
        void testSearchWithUnsortedOrderingReturnsAllRelyingParties() {
            Sort unsorted = Sort.unsorted();

            // there is no sorting to verify here, so just check that all RPs are present.
            Set<UUID> searchResultByUnknownSortKey =
                repository.searchRelyingPartyInstances(
                              "",
                              List.of(),
                              true,
                              false,
                              PageRequest.of(0, Integer.MAX_VALUE, unsorted))
                          .getContent()
                          .stream()
                          .map(BaseEntity::getId)
                          .collect(Collectors.toSet());

            Set<UUID> expectedResult =
                repository.findAll()
                          .stream()
                          .map(BaseEntity::getId)
                          .collect(Collectors.toSet());

            assertEquals(expectedResult, searchResultByUnknownSortKey);
        }
    }
}

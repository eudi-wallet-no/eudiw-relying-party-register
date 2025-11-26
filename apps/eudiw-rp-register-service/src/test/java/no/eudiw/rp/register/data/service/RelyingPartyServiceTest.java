package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.*;
import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
import static org.junit.jupiter.api.Assertions.*;

import no.eudiw.rp.register.data.RelyingPartyOrdering;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PagedModel;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
@DisplayName("When using the Relying Party Service")
@ActiveProfiles("junit")
public class RelyingPartyServiceTest {

    @Autowired
    private RelyingPartyService relyingPartyService;

    @Autowired
    private RelyingPartyRepository rpRepository;

    @Autowired
    private Converter converter;

    @BeforeEach
    void clearRepositoryBeforeEachTest() {
        rpRepository.deleteAll();
    }

    @DisplayName("When editing a relying party")
    @Nested
    class EditTests {

        @Test
        void testComplexEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            EditRelyingPartyResource editResource =
                generateEditRelyingPartyResource()
                    .withName(response.name())
                    .withPublicSector(response.publicSector());

            RelyingPartyResource editResponse1 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse1);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse1.relyingPartyEaas().size());

            RelyingPartyResource editResponse2 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse2);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse2.relyingPartyEaas().size());

            EditRelyingPartyResource emptyEaasEditResource =
                editResource.withRelyingPartyEaas(List.of());
            RelyingPartyResource editResponse3 = relyingPartyService.updateRelyingParty(
                    response.id(),
                    emptyEaasEditResource
            );
            assertNotNull(editResponse3);
            assertEquals(0, editResponse3.relyingPartyEaas().size());

            RelyingPartyResource getResult = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult);
            assertEquals(0, getResult.relyingPartyEaas().size());
        }

        @Test
        void testSimpleEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            EditRelyingPartyResource editResource =
                generateEditRelyingPartyResource()
                    .withName(response.name() + "new")
                    .withPublicSector(!response.publicSector());
            RelyingPartyResource editResponse = relyingPartyService.updateRelyingParty(
                    response.id(),
                    editResource
            );
            assertNotNull(editResponse);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse.relyingPartyEaas().size());
            assertEquals(response.name() + "new", editResponse.name());
            assertEquals(!response.publicSector(), editResponse.publicSector());

            RelyingPartyResource getResult = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult);
            assertEquals(editResource.relyingPartyEaas().size(), getResult.relyingPartyEaas().size());
            assertEquals(response.name() + "new", getResult.name());
            assertEquals(!response.publicSector(), getResult.publicSector());
        }

        @Test
        void testEaaEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            EditRelyingPartyResource editResource =
                generateEditRelyingPartyResource()
                    .withName(response.name())
                    .withPublicSector(response.publicSector());

            RelyingPartyResource editResponse1 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse1);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse1.relyingPartyEaas().size());

            RelyingPartyResource getResult1 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult1);
            assertEquals(editResource.relyingPartyEaas().size(), getResult1.relyingPartyEaas().size());

            RelyingPartyResource editResponse2 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse2);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse2.relyingPartyEaas().size());

            RelyingPartyResource getResult2 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult2);
            assertEquals(editResource.relyingPartyEaas().size(), getResult2.relyingPartyEaas().size());

            EditRelyingPartyResource emptyEaasEditResource =
                generateEditRelyingPartyResource()
                    .withRelyingPartyEaas(List.of());
            RelyingPartyResource editResponse3 =
                relyingPartyService.updateRelyingParty(response.id(), emptyEaasEditResource);
            assertNotNull(editResponse3);
            assertEquals(0, editResponse3.relyingPartyEaas().size());

            RelyingPartyResource getResult3 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult3);
            assertEquals(0, getResult3.relyingPartyEaas().size());

            RelyingPartyResource editResponse4 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse4);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse4.relyingPartyEaas().size());

            RelyingPartyResource getResult4 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult4);
            assertEquals(editResource.relyingPartyEaas().size(), getResult4.relyingPartyEaas().size());
        }
        
        @Test
        void testEaaAndEntitlementsEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            EditRelyingPartyResource editResource =
                generateEditRelyingPartyResource()
                    .withName(response.name())
                    .withPublicSector(response.publicSector());
            RelyingPartyResource editResponse1 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse1);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse1.relyingPartyEaas().size());

            RelyingPartyResource getResult1 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult1);
            assertEquals(editResource.relyingPartyEaas().size(), getResult1.relyingPartyEaas().size());
            assertEquals(editResource.relyingPartyEntitlements().size(), getResult1.relyingPartyEntitlements().size());

            EditRelyingPartyResource editResource2 =
                generateEditRelyingPartyResource()
                    .withName(response.name())
                    .withPublicSector(response.publicSector());

            RelyingPartyResource editResponse2 =
                relyingPartyService.updateRelyingParty(response.id(), editResource2);

            assertNotNull(editResponse2);
            assertEquals(editResource2.relyingPartyEaas().size(), editResponse2.relyingPartyEaas().size());
            assertEquals(editResource2.relyingPartyEntitlements().size(), editResponse2.relyingPartyEntitlements().size());

            RelyingPartyResource getResult2 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult2);
            assertEquals(editResource2.relyingPartyEaas().size(), getResult2.relyingPartyEaas().size());
            assertEquals(editResource2.relyingPartyEntitlements().size(), getResult2.relyingPartyEntitlements().size());
        }

        @Test
        void testDuplicateEaaEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);
            EditRelyingPartyResource editResource =
                generateEditRelyingPartyResource()
                    .withName(response.name())
                    .withPublicSector(response.publicSector());

            RelyingPartyResource editResponse1 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse1);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse1.relyingPartyEaas().size());

            RelyingPartyResource getResult1 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult1);
            assertEquals(editResource.relyingPartyEaas().size(), getResult1.relyingPartyEaas().size());
        }
    }

    @Nested
    @DisplayName("when searching for relying parties")
    class SearchTests {

        @Test
        @DisplayName("then only RPs with the specified entitlements are returned")
        void testSearchWithEntitlementsFiltering() {
            int numRelyingParties = 1000;
            List<RelyingParty> rpsIn = EntityGenerator.generateRelyingParties(numRelyingParties);
            rpRepository.saveAllAndFlush(rpsIn);

            List<RelyingPartyEntitlementResource> requiredEntitlements =
                rpsIn.getFirst()
                     .getRelyingPartyEntitlements()
                     .stream()
                     .map(converter::toResource)
                     .toList();

            Set<RelyingPartyResource> expectedSearchResult =
                rpsIn.stream()
                     .map(converter::toResource)
                     .filter(rp -> rp.relyingPartyEntitlements().containsAll(requiredEntitlements))
                     .collect(Collectors.toSet());

            SearchRelyingPartyResource searchResource =
                new SearchRelyingPartyResource()
                    .withIncludeInactive(true)
                    .withRequiredEntitlements(requiredEntitlements)
                    .withPageSize(numRelyingParties);

            Set<RelyingPartyResource> actualSearchResult =
                new HashSet<>(relyingPartyService.searchRelyingParties(searchResource).getContent());

            assertEquals(expectedSearchResult, actualSearchResult);
        }

        @Test
        @DisplayName("then expected sorting is correctly applied")
        void testSortingByCreatedMs() {
            int numRelyingParties = 100;
            List<RelyingParty> rpsIn = EntityGenerator.generateRelyingParties(numRelyingParties);
            rpRepository.saveAllAndFlush(rpsIn);

            SearchRelyingPartyResource searchResource =
                new SearchRelyingPartyResource().withIncludeInactive(true);

            List<RelyingPartyResource> searchResultByCreatedMs =
                relyingPartyService.searchRelyingParties(
                                       searchResource.withOrdering(RelyingPartyOrdering.CREATED_MS_ASC)
                                                     .withPageSize(numRelyingParties))
                                   .getContent();

            List<RelyingPartyResource> expectedResult =
                rpsIn.stream()
                     .map(converter::toResource)
                     .sorted(Comparator.comparing(RelyingPartyResource::createdMs))
                     .toList();

            assertEquals(expectedResult, searchResultByCreatedMs);
        }

        @Test
        @DisplayName("then search results returned in order when split into multiple requests")
        void testAllResultsReturnedInCorrectOrderAcrossMultipleServiceRequests() {
            int pageSize = 7;
            int numRelyingParties = 113;

            List<RelyingParty> rpsIn = EntityGenerator.generateRelyingParties(numRelyingParties);
            rpRepository.saveAllAndFlush(rpsIn);

            List<RelyingPartyResource> expectedSearchResult =
                rpRepository.findAll()
                            .stream()
                            .map(converter::toResource)
                            .sorted(Comparator.comparing(RelyingPartyResource::orgNr))
                            .toList();

            SearchRelyingPartyResource searchResourceOrderByOrgno =
                new SearchRelyingPartyResource().withPageSize(pageSize)
                                                .withOrdering(RelyingPartyOrdering.ORGNO_ASC);

            long numPages = relyingPartyService.searchRelyingParties(searchResourceOrderByOrgno)
                                               .getMetadata()
                                               .totalPages();
            List<RelyingPartyResource> actualSearchResult =
                IntStream.range(0, (int) numPages)
                         .mapToObj(searchResourceOrderByOrgno::withPage)
                         .map(relyingPartyService::searchRelyingParties)
                         .map(PagedModel::getContent)
                         .flatMap(List::stream)
                         .toList();

            assertEquals(expectedSearchResult, actualSearchResult);
        }
    }

    @Nested
    @DisplayName("when reading out relying parties with entitlements")
    class EntitlementDisplayNameTests {
        @Test
        @DisplayName("then each entitlement has a non-null display name")
        public void testAllEntitlementsHaveNonNullDisplayNames() {
            RelyingParty rpIn = EntityGenerator.generateRelyingPartyNoId();
            rpRepository.saveAndFlush(rpIn);

            RelyingPartyResource rpResourceOut = relyingPartyService.findRelyingParty(rpIn.getId());
            assertTrue(rpResourceOut.relyingPartyEntitlements()
                                    .stream()
                                    .map(RelyingPartyEntitlementResource::displayName)
                                    .noneMatch(Objects::isNull));
        }
    }
}

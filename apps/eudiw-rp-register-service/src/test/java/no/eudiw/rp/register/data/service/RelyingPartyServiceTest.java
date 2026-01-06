package no.eudiw.rp.register.data.service;

import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
import static org.junit.jupiter.api.Assertions.*;

import no.eudiw.rp.register.api.resource.relyingparty.*;
import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.repository.LegalEntityRepository;
import no.eudiw.rp.register.service.Converter;
import no.eudiw.rp.register.service.RelyingPartyService;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@DisplayName("When using the Relying Party Service")
@ActiveProfiles("junit")
public class RelyingPartyServiceTest {

    @Autowired
    private RelyingPartyService relyingPartyService;

    @Autowired
    private LegalEntityRepository rpRepository;

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
                generateEditRelyingPartyResource().withTradeName(response.tradeName());

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
                generateEditRelyingPartyResource().withTradeName(response.tradeName() + "new");
            RelyingPartyResource editResponse = relyingPartyService.updateRelyingParty(
                    response.id(),
                    editResource
            );
            assertNotNull(editResponse);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse.relyingPartyEaas().size());
            assertEquals(response.tradeName() + "new", editResponse.tradeName());
            assertEquals(response.publicSector(), editResponse.publicSector());

            RelyingPartyResource getResult = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult);
            assertEquals(editResource.relyingPartyEaas().size(), getResult.relyingPartyEaas().size());
            assertEquals(response.publicSector(), getResult.publicSector());
            assertEquals(response.tradeName() + "new", getResult.tradeName());
            assertEquals(response.publicSector(), getResult.publicSector());
        }

        @Test
        void testEaaEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            EditRelyingPartyResource editResource =
                generateEditRelyingPartyResource().withTradeName(response.tradeName());

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
                generateEditRelyingPartyResource().withRelyingPartyEaas(List.of());
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
                generateEditRelyingPartyResource().withTradeName(response.tradeName());
            RelyingPartyResource editResponse1 =
                relyingPartyService.updateRelyingParty(response.id(), editResource);
            assertNotNull(editResponse1);
            assertEquals(editResource.relyingPartyEaas().size(), editResponse1.relyingPartyEaas().size());

            RelyingPartyResource getResult1 = relyingPartyService.findRelyingParty(response.id());
            assertNotNull(getResult1);
            assertEquals(editResource.relyingPartyEaas().size(), getResult1.relyingPartyEaas().size());
            assertEquals(editResource.relyingPartyEntitlements().size(), getResult1.relyingPartyEntitlements().size());

            EditRelyingPartyResource editResource2 =
                generateEditRelyingPartyResource().withTradeName(response.tradeName());

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
                generateEditRelyingPartyResource().withTradeName(response.tradeName());

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
            int numLegalEntities = 10;
            List<LegalEntity> legalEntities = EntityGenerator.generateLegalEntities(numLegalEntities);
            rpRepository.saveAllAndFlush(legalEntities);

            List<String> requiredEntitlements =
                legalEntities.getFirst().getRelyingPartyInstances().getFirst()
                             .getRelyingPartyEntitlements()
                             .stream()
                             .map(RelyingPartyEntitlement::getEntitlement)
                             .toList();

            Set<RelyingPartyResource> expectedSearchResult =
                legalEntities.stream()
                             .flatMap(le -> le.getRelyingPartyInstances().stream())
                             .map(converter::toResource)
                             .filter(rp -> rp.relyingPartyEntitlements()
                                             .stream()
                                             .map(RelyingPartyEntitlementResource::entitlement)
                                             .toList()
                                             .containsAll(requiredEntitlements))
                             .collect(Collectors.toSet());

            SearchRelyingPartyResource searchResource =
                new SearchRelyingPartyResource()
                    .withIncludeInactive(true)
                    .withRequiredEntitlements(requiredEntitlements)
                    .withPageSize(Integer.MAX_VALUE);

            Set<RelyingPartyResource> actualSearchResult =
                new HashSet<>(relyingPartyService.searchRelyingParties(searchResource).getContent());

            assertEquals(expectedSearchResult, actualSearchResult);
        }
    }

    @Nested
    @DisplayName("when reading out relying parties with entitlements")
    class EntitlementDisplayNameTests {
        @Test
        @DisplayName("then each entitlement has a non-null display name")
        public void testAllEntitlementsHaveNonNullDisplayNames() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(
                List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")));

            assertTrue(legalEntity.getRelyingPartyInstances().getFirst().getAccessCertificates().isEmpty());
            rpRepository.saveAndFlush(legalEntity);

            RelyingPartyResource rpResourceOut = relyingPartyService.findRelyingParty(legalEntity.getRelyingPartyInstances().get(0).getId());
            assertTrue(rpResourceOut.relyingPartyEntitlements()
                .stream()
                .map(RelyingPartyEntitlementResource::displayName)
                .noneMatch(Objects::isNull));
        }
    }
}

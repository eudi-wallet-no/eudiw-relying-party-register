package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.*;
import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
import static org.junit.jupiter.api.Assertions.*;

import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@DisplayName("When using the Relying Party Service")
@ActiveProfiles("test")
public class RelyingPartyServiceTest {

    @Autowired
    private RelyingPartyService relyingPartyService;

    @Autowired
    private RelyingPartyRepository rpRepository;

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
            int nRelyingParties = 1000;
            List<RelyingParty> rpsIn = EntityGenerator.generateRelyingParties(nRelyingParties);
            rpRepository.saveAllAndFlush(rpsIn);

            List<RelyingPartyEntitlementResource> requiredEntitlements =
                rpsIn.getFirst()
                     .getRelyingPartyEntitlements()
                     .stream()
                     .map(Converter::toResource)
                     .toList();

            Set<RelyingPartyResource> expectedSearchResult =
                rpsIn.stream()
                     .map(Converter::toResource)
                     .filter(rp -> rp.relyingPartyEntitlements().containsAll(requiredEntitlements))
                     .collect(Collectors.toSet());

            SearchRelyingPartyResource searchResource =
                new SearchRelyingPartyResource("", true, requiredEntitlements, 0, 1000);
            Set<RelyingPartyResource> actualSearchResult =
                new HashSet<>(relyingPartyService.searchRelyingParties(searchResource).getContent());

            assertEquals(expectedSearchResult, actualSearchResult);
        }
    }
}

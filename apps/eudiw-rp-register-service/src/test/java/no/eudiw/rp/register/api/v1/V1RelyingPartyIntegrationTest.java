package no.eudiw.rp.register.api.v1;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.api.v1.resource.relyingparty.*;
import no.eudiw.rp.register.domain.LegalEntity;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.LegalEntityRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PagedModel;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static no.eudiw.rp.register.testdata.TestDataGenerator.generateName;
import static no.eudiw.rp.register.testdata.TestDataGenerator.generateValidOrgno;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When using the V1 relying party API service")
@ActiveProfiles("junit")
public class V1RelyingPartyIntegrationTest {

    @Autowired
    private V1ApiService relyingPartyService;

    @Autowired
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testCreateRelyingParty() {
        CreateRelyingPartyResource createRelyingPartyResource = new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            List.of(),
            List.of()
        );

        RelyingPartyResource result = relyingPartyService.createRelyingParty(createRelyingPartyResource);

        assertNotNull(result);
        assertEquals(result.tradeName(), createRelyingPartyResource.tradeName());

        RelyingPartyInstance instance = relyingPartyInstanceRepository.findById(result.id()).get();

        assertNotNull(instance);
        assertNotEquals(result.id(), instance.getLegalEntity().getId());
        assertEquals(result.publicSector(), instance.getLegalEntity().isPublicSector());
    }

    @Test
    void testFindRelyingParty() {
        CreateRelyingPartyResource createRelyingPartyResource = new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            List.of(),
            List.of()
        );

        RelyingPartyResource result = relyingPartyService.createRelyingParty(createRelyingPartyResource);

        RelyingPartyResource getResult = relyingPartyService.findRelyingParty(result.id());

        assertEquals(result, getResult);
    }

    @Test
    void testDeleteRelyingParty() {
        CreateRelyingPartyResource createRelyingPartyResource = new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            List.of(),
            List.of()
        );

        RelyingPartyResource result = relyingPartyService.createRelyingParty(createRelyingPartyResource);
        RelyingPartyInstance instance = relyingPartyInstanceRepository.findById(result.id()).orElseThrow();
        LegalEntity legalEntity = instance.getLegalEntity();

        relyingPartyService.deleteRelyingParty(result.id());
        entityManager.clear();

        assertTrue(relyingPartyInstanceRepository.findById(result.id()).isEmpty());
        assertTrue(legalEntityRepository.findById(legalEntity.getId()).isPresent());
        assertThrows(
            NotFoundException.class,
            () -> relyingPartyService.deleteRelyingParty(result.id())
        );
    }

    @Test
    void testStoreRelyingParty() {
        CreateRelyingPartyResource createRelyingPartyResource = new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            List.of(),
            List.of()
        );

        RelyingPartyResource result = relyingPartyService.createRelyingParty(createRelyingPartyResource);

        RelyingPartyResource getResult = relyingPartyService.findRelyingParty(result.id());

        assertEquals(result, getResult);
    }

    @Test
    void testSearchRelyingPartyOnName() {
        CreateRelyingPartyResource createRelyingPartyResource = new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            List.of(),
            List.of()
        );

        RelyingPartyResource result = relyingPartyService.createRelyingParty(createRelyingPartyResource);

        PagedModel<RelyingPartyResource> searchResult = relyingPartyService.searchRelyingParties(
            new SearchRelyingPartyResource(createRelyingPartyResource.tradeName())
        );
        
        assertEquals(searchResult.getContent().getFirst(), result);
    }

    @Test
    void testEditRelyingPartyOnName() {
        var entitlements = List.of(new RelyingPartyEntitlementResource(
            "https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
            "QEAA Provider",
            null));
        CreateRelyingPartyResource createRelyingPartyResource = new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            entitlements,
            List.of()
        );

        RelyingPartyResource createResult = relyingPartyService.createRelyingParty(createRelyingPartyResource);

        EditRelyingPartyResource editResource = new EditRelyingPartyResource(
            generateName(),
            entitlements,
            List.of(),
            true
        );

        RelyingPartyResource editResult = relyingPartyService.updateRelyingParty(createResult.id(), editResource);
        assertEquals(editResult.tradeName(), editResource.tradeName());
    }

}

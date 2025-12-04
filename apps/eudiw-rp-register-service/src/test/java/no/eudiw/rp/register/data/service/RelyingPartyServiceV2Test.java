package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.RelyingPartyInstance;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PagedModel;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static no.eudiw.rp.register.testdata.TestDataGenerator.generateName;
import static no.eudiw.rp.register.testdata.TestDataGenerator.generateValidOrgno;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When using the Relying Party Service V2")
@ActiveProfiles("junit")
public class RelyingPartyServiceV2Test {

    @Autowired
    private RelyingPartyService relyingPartyService;

    @Autowired
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;

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
            null,
            new ArrayList<>()));
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

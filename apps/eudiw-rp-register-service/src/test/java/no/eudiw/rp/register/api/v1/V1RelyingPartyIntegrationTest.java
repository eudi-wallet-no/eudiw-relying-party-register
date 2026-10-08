package no.eudiw.rp.register.api.v1;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.api.v1.resource.relyingparty.*;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyServiceRepository;
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
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @Autowired
    private WalletRelyingPartyServiceRepository walletRelyingPartyServiceRepository;

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
        assertNotEquals(result.id(), instance.getWalletRelyingPartyService().getWalletRelyingParty().getId());
        assertEquals(result.publicSector(), instance.getWalletRelyingPartyService().getWalletRelyingParty().isPsb());
        assertNotEquals(result.id(), instance.getWalletRelyingPartyService().getId());
        assertEquals(List.of(instance), instance.getWalletRelyingPartyService().getRelyingPartyInstances());
        assertTrue(walletRelyingPartyServiceRepository.existsById(instance.getWalletRelyingPartyService().getId()));
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
        WalletRelyingParty walletRelyingParty = instance.getWalletRelyingPartyService().getWalletRelyingParty();
        var serviceId = instance.getWalletRelyingPartyService().getId();

        relyingPartyService.deleteRelyingParty(result.id());
        entityManager.clear();

        assertTrue(relyingPartyInstanceRepository.findById(result.id()).isEmpty());
        assertFalse(walletRelyingPartyServiceRepository.existsById(serviceId));
        assertTrue(walletRelyingPartyRepository.findById(walletRelyingParty.getId()).isPresent());
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

    @Test
    void nameOnlyUpdatePersistsInstanceTimestampAndPreservesSiblingService() {
        String orgno = generateValidOrgno();
        RelyingPartyResource first = relyingPartyService.createRelyingParty(
            new CreateRelyingPartyResource(orgno, "First service", List.of(), List.of()));
        RelyingPartyResource sibling = relyingPartyService.createRelyingParty(
            new CreateRelyingPartyResource(orgno, "Sibling service", List.of(), List.of()));
        var instance = relyingPartyInstanceRepository.findById(first.id()).orElseThrow();
        var serviceId = instance.getWalletRelyingPartyService().getId();
        var partyId = instance.getWalletRelyingPartyService().getWalletRelyingParty().getId();

        RelyingPartyResource updated = relyingPartyService.updateRelyingParty(first.id(),
            new EditRelyingPartyResource("Renamed service", List.of(), List.of(), true));
        entityManager.clear();
        RelyingPartyResource reloaded = relyingPartyService.findRelyingParty(first.id());

        assertEquals(updated, reloaded);
        assertEquals(first.createdMs(), reloaded.createdMs());
        assertTrue(reloaded.lastUpdatedMs() > first.lastUpdatedMs());
        assertEquals("Renamed service", reloaded.tradeName());
        assertEquals(sibling, relyingPartyService.findRelyingParty(sibling.id()));
        assertEquals(serviceId, relyingPartyInstanceRepository.findById(first.id()).orElseThrow()
            .getWalletRelyingPartyService().getId());

        relyingPartyService.deleteRelyingParty(first.id());
        entityManager.clear();
        assertFalse(walletRelyingPartyServiceRepository.existsById(serviceId));
        assertTrue(walletRelyingPartyRepository.existsById(partyId));
        assertEquals(sibling, relyingPartyService.findRelyingParty(sibling.id()));
        relyingPartyService.deleteRelyingParty(sibling.id());
    }

}

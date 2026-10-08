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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    void testDeleteOnlySelectedInstanceAndCleanUpServiceWhenEmpty() {
        RelyingPartyResource result = relyingPartyService.createRelyingParty(
            new CreateRelyingPartyResource(generateValidOrgno(), generateName(), List.of(), List.of())
        );
        RelyingPartyInstance instance = relyingPartyInstanceRepository.findById(result.id()).orElseThrow();
        var serviceId = instance.getWalletRelyingPartyService().getId();
        var walletRelyingPartyId = instance.getWalletRelyingPartyService().getWalletRelyingParty().getId();
        var siblingId = new TransactionTemplate(transactionManager).execute(status -> {
            var walletService = walletRelyingPartyServiceRepository.findById(serviceId).orElseThrow();
            var sibling = new RelyingPartyInstance(List.of(), List.of(), List.of());
            sibling.setWalletRelyingPartyService(walletService);
            walletService.getRelyingPartyInstances().add(sibling);
            return relyingPartyInstanceRepository.saveAndFlush(sibling).getId();
        });
        RelyingPartyResource siblingBeforeDeletion = relyingPartyService.findRelyingParty(siblingId);

        relyingPartyService.deleteRelyingParty(result.id());
        entityManager.clear();

        assertFalse(relyingPartyInstanceRepository.existsById(result.id()));
        assertEquals(siblingBeforeDeletion, relyingPartyService.findRelyingParty(siblingId));
        var remainingService = walletRelyingPartyServiceRepository.findById(serviceId).orElseThrow();
        assertEquals(List.of(siblingId),
            remainingService.getRelyingPartyInstances().stream().map(RelyingPartyInstance::getId).toList());
        assertTrue(walletRelyingPartyRepository.findById(walletRelyingPartyId).orElseThrow()
            .getServices().stream().anyMatch(service -> serviceId.equals(service.getId())));

        relyingPartyService.deleteRelyingParty(siblingId);
        entityManager.clear();

        assertFalse(relyingPartyInstanceRepository.existsById(siblingId));
        assertFalse(walletRelyingPartyServiceRepository.existsById(serviceId));
        assertTrue(walletRelyingPartyRepository.findById(walletRelyingPartyId).orElseThrow()
            .getServices().isEmpty());
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

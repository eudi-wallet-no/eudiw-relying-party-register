package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.etsi119602.pojo.PkiOb;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntity;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("ACA Trustlist is generated with content")
class TrustlistACAGeneratorServiceTest {

    @Autowired
    TrustlistACAGeneratorService trustListACAGeneratorService;

    @Test
    void generateTrustlistACA() {
        LoTE loTE = trustListACAGeneratorService.generateTrustlistACA();
        assertNotNull(loTE);
    }

    @Test
    void contentInTrustListACAIsCorrect() {
        LoTE lote = trustListACAGeneratorService.generateTrustlistACA();
        assertNotNull(lote);
        assertEquals("NO", lote.getListAndSchemeInformation().getSchemeTerritory());
        assertEquals("http://uri.etsi.org/19602/LoTEType/EUWRPACProvidersList", lote.getListAndSchemeInformation().getLoTEType().toString());
        assertNotNull(lote.getListAndSchemeInformation().getListIssueDateTime());
        assertNotNull(lote.getListAndSchemeInformation().getLoTESequenceNumber());
        assertNotNull(lote.getListAndSchemeInformation().getSchemeName());
        assertNotNull(lote.getListAndSchemeInformation().getLoTEVersionIdentifier());
        assertNotNull(lote.getListAndSchemeInformation().getNextUpdate());
        verifyTrustedEntitiesHasContent(lote.getTrustedEntitiesList());

    }

    private static void verifyTrustedEntitiesHasContent(List<TrustedEntity> trustedEntities) {
        assertFalse(trustedEntities.isEmpty());
        for (TrustedEntity te : trustedEntities) {
            assertNotNull(te);
            assertNotNull(te.getTrustedEntityInformation().getTEInformationURI());
            assertNotNull(te.getTrustedEntityInformation().getTEName());
            assertFalse(te.getTrustedEntityInformation().getTEName().isEmpty());
            assertNotNull(te.getTrustedEntityInformation().getTEAddress());
            assertNotNull(te.getTrustedEntityInformation().getTEInformationURI());

            verifyTrustedEntityServicesHasContent(te.getTrustedEntityServices());
        }
    }

    private static void verifyTrustedEntityServicesHasContent(List<TrustedEntityService> teServices) {
        assertNotNull(teServices);
        assertFalse(teServices.isEmpty(), "TSPServices should not be empty");
        assertEquals(2, teServices.size(), "There should be exactly two TrustedEntityService in the list");
        for (TrustedEntityService service : teServices) {
            assertNotNull(service);
            assertNotNull(service.getServiceInformation());
            assertNotNull(service.getServiceInformation().getServiceName());
            assertFalse(service.getServiceInformation().getServiceName().isEmpty());
            assertNotNull(service.getServiceInformation().getServiceDigitalIdentity().getX509Certificates());
            assertEquals(PkiOb.class, service.getServiceInformation().getServiceDigitalIdentity().getX509Certificates().getFirst().getClass());
        }
    }

}
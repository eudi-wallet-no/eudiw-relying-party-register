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
@DisplayName("PID Trustlist is generated with content")
public class TrustlistPIDGeneratorServiceTest {


    @Autowired
    TrustlistPIDGeneratorService trustListPIDGeneratorService;

    @Test
    void generateTrustlistPID() {
        LoTE loTE = trustListPIDGeneratorService.generateTrustlistPID();
        assertNotNull(loTE);
    }


    @Test
    void contentInTrustListPIDIsCorrect() {
        LoTE lote = trustListPIDGeneratorService.generateTrustlistPID();
        assertNotNull(lote);
        assertEquals("NO", lote.getListAndSchemeInformation().getSchemeTerritory());
        assertEquals("http://uri.etsi.org/19602/LoTEType/EUPIDProvidersList", lote.getListAndSchemeInformation().getLoTEType().toString());
        assertNotNull(lote.getListAndSchemeInformation().getListIssueDateTime());
        assertNotNull(lote.getListAndSchemeInformation().getLoTESequenceNumber());
        assertEquals("Tillitsliste for Personal Identification Data tilbydere i eidas2sandkasse i junit", lote.getListAndSchemeInformation().getSchemeName().getFirst().getValue());
        assertNotNull(lote.getListAndSchemeInformation().getLoTEVersionIdentifier());
        assertNotNull(lote.getListAndSchemeInformation().getNextUpdate());
        verifyTrustedEntityHasContent(lote.getTrustedEntitiesList());

    }

    private static void verifyTrustedEntityHasContent(List<TrustedEntity> trustedEntities) {
        assertFalse(trustedEntities.isEmpty());
        for (TrustedEntity te : trustedEntities) {
            assertNotNull(te);
            assertEquals("DIGITALISERINGSDIREKTORATET", te.getTrustedEntityInformation().getTEName().getFirst().getValue());
            assertNotNull(te.getTrustedEntityInformation().getTEInformationURI());
            assertFalse(te.getTrustedEntityInformation().getTEName().isEmpty());
            assertNotNull(te.getTrustedEntityInformation().getTEAddress());
            assertEquals(6, te.getTrustedEntityInformation().getTEAddress().getTEElectronicAddress().size()); // Phone number is also a part in pid, therefore six
            assertNotNull(te.getTrustedEntityInformation().getTEInformationURI());

            verify1TrustedEntityServiceHasContent(te.getTrustedEntityServices());
        }
    }

    private static void verify1TrustedEntityServiceHasContent(List<TrustedEntityService> teServices) {
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

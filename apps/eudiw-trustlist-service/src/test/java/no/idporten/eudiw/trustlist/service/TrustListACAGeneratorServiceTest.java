package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.config.TrustListACAProperties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
class TrustListACAGeneratorServiceTest {

    @Autowired
    TrustListACAProperties acaProperties;

    @Test
    void generateTrustlistACA() {
        TrustListACAGeneratorService service = new TrustListACAGeneratorService(acaProperties);
        LoTE loTE = service.generateTrustlistACA();
        assertNotNull(loTE);
    }
}
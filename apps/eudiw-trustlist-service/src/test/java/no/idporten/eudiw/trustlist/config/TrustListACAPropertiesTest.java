package no.idporten.eudiw.trustlist.config;

import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigInteger;

import static no.idporten.eudiw.trustlist.config.TrustList612PropertiesTest.verifySchemeInformationHasContent;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
class TrustListACAPropertiesTest {

    @Autowired
    private TrustListACAProperties properties;

    @Test
    void pathIsNotEmpty() {
        assertNotNull(properties);
        assertNotNull(properties.path());
        assertFalse(properties.path().isEmpty(), "Path should not be empty");
    }

    @Test
    void schemeInformationHasContentForTrustlist() {
        assertNotNull(properties);
        TLSchemeInformation tlSchemeInformation = properties.schemeInformation();
        verifySchemeInformationHasContent(tlSchemeInformation);
    }


}
package no.idporten.eudiw.trustlist.config;

import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
class TrustList612PropertiesTest {

    @Autowired
    private TrustList612Properties properties;

    @Test
    void pathIsNotEmpty() {
        assertNotNull(properties);
        assertNotNull(properties.trustlistPath());
        assertFalse(properties.trustlistPath().isEmpty(), "Path should not be empty");
    }
    @Test
    void xtslPathIsNotEmpty() {
        assertNotNull(properties);
        assertNotNull(properties.trustlistPathXtsl());
        assertFalse(properties.trustlistPathXtsl().isEmpty(), "PathXtsl should not be empty");
    }

    @Test
    void shaPathIsNotEmpty() {
        assertNotNull(properties);
        assertNotNull(properties.trustlistPathSha2());
        assertFalse(properties.trustlistPathSha2().isEmpty(), "Path sha should not be empty");
    }

    @Test
    void schemeInformationHasContentForTrustlist() {
        assertNotNull(properties);
        TLSchemeInformation tlSchemeInformation = properties.schemeInformation();
        verifySchemeInformationHasContent(tlSchemeInformation);
    }

    protected static void verifySchemeInformationHasContent(TLSchemeInformation tlSchemeInformation) {
        assertNotNull(tlSchemeInformation);

        //Name
        assertNotNull(tlSchemeInformation.schemeName());
        assertNotNull(tlSchemeInformation.schemeName().langNo());
        assertFalse(tlSchemeInformation.schemeName().langNo().isEmpty(), "Scheme name no should not be empty");
        assertNotNull(tlSchemeInformation.schemeName().langEn());
        assertFalse(tlSchemeInformation.schemeName().langEn().isEmpty(), "Scheme name en should not be empty");

        //Sequence number
        assertNotNull(tlSchemeInformation.sequenceNumber());
        assertEquals(1, tlSchemeInformation.sequenceNumber().compareTo(BigInteger.ZERO), "Sequence number should be greater than 0");

        // List issue date time
        assertNotNull(tlSchemeInformation.listIssueDateTime());
    }
}
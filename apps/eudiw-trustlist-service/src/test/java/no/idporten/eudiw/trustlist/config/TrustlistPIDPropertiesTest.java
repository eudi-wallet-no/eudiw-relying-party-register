package no.idporten.eudiw.trustlist.config;


import no.idporten.eudiw.trustlist.domain.etsi602.ListAndSchemeInformation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("junit")
class TrustlistPIDPropertiesTest {

    @Autowired
    private TrustlistPIDProperties properties;

    @Test
    void pathIsNotEmpty() {
        assertNotNull(properties);
        assertNotNull(properties.path());
        assertFalse(properties.path().isEmpty(), "Path should not be empty");
    }

    @Test
    void schemeInformationHasContentForTrustlistPID() {
        assertNotNull(properties);
        ListAndSchemeInformation tlSchemeInformation = properties.schemeInformation();
        assertNotNull(tlSchemeInformation);
        assertNotNull(tlSchemeInformation.schemeName());
        assertNotNull(tlSchemeInformation.listIssueDateTime());
        assertNotNull(tlSchemeInformation.informationUris());
        assertNotNull(tlSchemeInformation.schemeTypeCommunityRules());
        assertNotNull(tlSchemeInformation.loteType());
        assertNotNull(tlSchemeInformation.sequenceNumber());
        assertNotNull(tlSchemeInformation.statusDeterminationApproach());
        assertNotNull(tlSchemeInformation.schemeName());
        assertFalse(tlSchemeInformation.schemeName().langEn().isEmpty(), "scheme name EN should not be empty");
        assertFalse(tlSchemeInformation.schemeName().langNo().isEmpty(), "scheme name NO should not be empty");
        assertNotNull(tlSchemeInformation.informationUris(), "scheme informationUris should not be null");
        assertNotNull(tlSchemeInformation.informationUris().langNo(), "scheme informationUri for NO should not be null");
        assertNotNull(tlSchemeInformation.informationUris().langEn(), "scheme informationUri for EN should not be null");
        assertFalse(tlSchemeInformation.schemeTypeCommunityRules().isEmpty(), "scheme type community rules should not be empty");
        assertNotNull(tlSchemeInformation.loteType(), "lote type should not be empty");
        assertNotEquals(tlSchemeInformation.sequenceNumber(), new BigInteger(String.valueOf(0)), "sequence number in scheme information should not be zero");
        assertNotNull(tlSchemeInformation.statusDeterminationApproach(), "status determination approach should not be empty");


    }


}
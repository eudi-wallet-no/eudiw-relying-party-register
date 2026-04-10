package no.idporten.eudiw.trustlist.service;

import com.nimbusds.jose.JWSObject;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.text.ParseException;

import static no.idporten.eudiw.trustlist.TestDataGenerator.DIGDIR;
import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_ACA;
import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_PID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("602 Trustlists are only generated once on initialization of service")
class Trustlist602InitializationTest {

    @MockitoSpyBean
    private JsonSignerService signerService;

    @Autowired
    private Trustlist602Service trustlist602Service;

    @MockitoSpyBean
    private Trustlist602GeneratorService generatorService;

    @Test
    @DisplayName("getSignedPidTrustlist() or getSignedACATrustlist() should not trigger new generation of trustlists")
    void verify602TrustlistsAreOnlyGeneratedOnce() throws ParseException {

        // 1. Pid call
        String signedPidTrustlist1 = trustlist602Service.getSignedTrustlist(TSL_PID);
        verifyContent(signedPidTrustlist1);

        // 2. Pid call (same content as 1. call)
        String signedPidTrustlist2 = trustlist602Service.getSignedTrustlist(TSL_PID);
        verifyContent(signedPidTrustlist2);

        assertEquals(signedPidTrustlist1, signedPidTrustlist2);

        // 1. ACA call
        String signedAcaTrustlist1 = trustlist602Service.getSignedTrustlist(TSL_ACA);
        verifyContent(signedAcaTrustlist1);

        // 2. ACA call (same content as 1. call)
        String signedAcaTrustlist2 = trustlist602Service.getSignedTrustlist(TSL_ACA);
        verifyContent(signedAcaTrustlist2);

        assertEquals(signedAcaTrustlist1, signedAcaTrustlist2);

        // only called on postConstruct of class (initialization), never when calling method service.getSignedPidTrustlist();
        verify(generatorService, times(1)).generateTrustlist(eq(TSL_PID));

        verify(generatorService, times(1)).generateTrustlist(eq(TSL_ACA));

        ArgumentCaptor<LoTE> pidLoTECaptor = ArgumentCaptor.forClass(LoTE.class);
        verify(signerService, times(1)).signedTrustlist(pidLoTECaptor.capture(), eq("signing-602-pid"));
        LoTE pidCapturedLoTE = pidLoTECaptor.getValue();
        assertNotNull(pidCapturedLoTE.getListAndSchemeInformation());
        assertEquals("http://uri.etsi.org/19602/LoTEType/EUPIDProvidersList",
                pidCapturedLoTE.getListAndSchemeInformation().getLoTEType().toString());

        ArgumentCaptor<LoTE> acaLoTECaptor = ArgumentCaptor.forClass(LoTE.class);
        verify(signerService, times(1)).signedTrustlist(acaLoTECaptor.capture(), eq("signing-602"));
        LoTE acaLoTECaptorValue = acaLoTECaptor.getValue();
        assertNotNull(acaLoTECaptorValue.getListAndSchemeInformation());
        assertEquals("http://uri.etsi.org/19602/LoTEType/EUWRPACProvidersList",
                acaLoTECaptorValue.getListAndSchemeInformation().getLoTEType().toString());
    }

    private static void verifyContent(String signedTrustlist) throws ParseException {
        assertNotNull(signedTrustlist);
        JWSObject jwsObject = JWSObject.parse(signedTrustlist);
        assertNotNull(jwsObject);
        String body = jwsObject.getPayload().toString();
        assertTrue(body.contains(DIGDIR)); // just check some expected content in payload
    }

}
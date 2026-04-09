package no.idporten.eudiw.trustlist.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import no.idporten.eudiw.trustlist.TestDataGenerator;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.domain.etsi602.Trustlist;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.time.Instant;

import static no.idporten.eudiw.trustlist.TestDataGenerator.DIGDIR;
import static no.idporten.eudiw.trustlist.TestDataGenerator.createJsonFromLoTE;
import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_PID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When call 602 Pid trustlist service")
class TrustlistPIDServiceTest {

    @Mock
    private JsonSignerService signerService;

    @Mock
    private Trustlist602GeneratorService generatorService;

    @Mock
    private Trustlist602Properties trustlist602Properties;

    @InjectMocks
    private TrustlistPIDService service;

    @BeforeEach
    void setup() {
        LoTE loTETrustlist = TestDataGenerator.createLoTETrustlist();
        when(generatorService.generateTrustlist(TSL_PID)).thenReturn(loTETrustlist);
    }


    @Test
    @DisplayName("then getPIDTrustlistAsLoTE returns LoTE with ListAndSchemeInformation and TrustedEntitiesList with content")
    void getPIDTrustlistAsLoTE() {
        LoTE loTE = service.getPIDTrustlistAsLoTE();
        assertNotNull(loTE);
        assertNotNull(loTE.getListAndSchemeInformation());
        assertNotNull(loTE.getListAndSchemeInformation().getLoTEType());
        assertNotNull(loTE.getTrustedEntitiesList());
        verify(generatorService, atLeastOnce()).generateTrustlist(eq(TSL_PID));
    }

    @Test
    @DisplayName("then getSignedPidTrustlist returns signed trustlist with expected content in payload")
    void getSignedPidTrustlist() throws ParseException {

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .customParam("iat", Instant.now().getEpochSecond())
                .build();

        Payload payload = new Payload(createJsonFromLoTE());
        String jwtWithoutSignature = header.toBase64URL().toString() + "." + payload.toBase64URL().toString() + ".signature";

        when(trustlist602Properties.getPidTrustlist()).thenReturn(new Trustlist("path","signing-602", null, null));
        when(signerService.signedTrustlist(any(LoTE.class), anyString())).thenReturn(jwtWithoutSignature);

        String signedTrustlist1 = service.signedPidJson();
        verifySignedTrustlistResult(signedTrustlist1);

        verify(trustlist602Properties, only()).getPidTrustlist();
        verify(generatorService, only()).generateTrustlist(eq(TSL_PID));

        ArgumentCaptor<LoTE> loTECaptor = ArgumentCaptor.forClass(LoTE.class);
        verify(signerService, only()).signedTrustlist(loTECaptor.capture(), anyString());
        LoTE capturedLoTE = loTECaptor.getValue();
        assertNotNull(capturedLoTE.getListAndSchemeInformation());
        assertEquals("http://acaorpid-trustlist-type",
                capturedLoTE.getListAndSchemeInformation().getLoTEType().toString());
    }

    private static void verifySignedTrustlistResult(String signedTrustlist) throws ParseException {
        assertNotNull(signedTrustlist);
        JWSObject jwsObject = JWSObject.parse(signedTrustlist);
        assertNotNull(jwsObject);
        String body = jwsObject.getPayload().toString();
        assertTrue(body.contains(DIGDIR)); // just check some expected content in payload
    }

}
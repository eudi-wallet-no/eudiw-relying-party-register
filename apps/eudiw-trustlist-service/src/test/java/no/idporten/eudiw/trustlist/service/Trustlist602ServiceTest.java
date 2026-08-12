package no.idporten.eudiw.trustlist.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import no.idporten.eudiw.trustlist.TestDataGenerator;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.domain.etsi602.Trustlist;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static no.idporten.eudiw.trustlist.TestDataGenerator.*;
import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When get 602 trustlist")
class Trustlist602ServiceTest {

    @Mock
    private Trustlist602GeneratorService generatorService;

    @Mock
    private JsonSignerService jsonSignerService;

    @Mock
    private Trustlist602Properties trustlist602Properties;

    @InjectMocks
    private Trustlist602Service signed602TrustlistService;

    private static final String keystoreNamePid = "signing-pid-602";
    private static final String keystoreNameAca = "signing-aca-602";


    @AfterEach
    void tearDown() {
        reset(generatorService);
    }


    @Test
    @DisplayName("then generateSignedTrustlists returns all 602 trustlists as signed json")
    void getSignedTrustlists() throws ParseException {
        when(trustlist602Properties.tsl602()).thenReturn(createTrustlistMapFromProperties());
        when(generatorService.generateTrustlist(anyString())).thenReturn(TestDataGenerator.createLoTETrustlist());

        LoTE loTETrustlistAca = createLoTETrustlist("http://aca-trustlist-type");
        String signedJsonAca = getJwtWithValidHeaderAndPayload(loTETrustlistAca);
        LoTE loTETrustlistPid = createLoTETrustlist("http://pid-trustlist-type");
        String signedJsonPid = getJwtWithValidHeaderAndPayload(loTETrustlistPid);
        when(generatorService.generateTrustlist(eq(TSL_ACA))).thenReturn(loTETrustlistAca);
        when(generatorService.generateTrustlist(eq(TSL_PID))).thenReturn(loTETrustlistPid);

        when(jsonSignerService.signedTrustlist(any(LoTE.class), eq(keystoreNameAca))).thenReturn(signedJsonAca);
        when(jsonSignerService.signedTrustlist(any(LoTE.class), eq(keystoreNamePid))).thenReturn(signedJsonPid);

        Map<String, String> signedTrustlists = signed602TrustlistService.generateSignedTrustlists();
        assertNotNull(signedTrustlists);
        assertNotNull(signedTrustlists.get(TSL_ACA));
        verifySignedTrustlistResult(signedTrustlists.get(TSL_ACA));
        assertEquals(signedJsonAca, signedTrustlists.get(TSL_ACA));
        assertNotNull(signedTrustlists.get(TSL_PID));
        assertEquals(signedJsonPid, signedTrustlists.get(TSL_PID));

        verifyJsonSignerService(keystoreNameAca, "http://aca-trustlist-type");
        verifyJsonSignerService(keystoreNamePid, "http://pid-trustlist-type");
    }

    private void verifyJsonSignerService(String keystoreName, String trustlistTypeUrl) {
        ArgumentCaptor<LoTE> pidLoTeCaptor = ArgumentCaptor.forClass(LoTE.class);
        verify(jsonSignerService, times(1)).signedTrustlist(pidLoTeCaptor.capture(), eq(keystoreName));
        LoTE capturedLoTE = pidLoTeCaptor.getValue();
        assertNotNull(capturedLoTE.getListAndSchemeInformation());
        assertEquals(trustlistTypeUrl,
                capturedLoTE.getListAndSchemeInformation().getLoTEType().toString());
    }


    @ParameterizedTest
    @ValueSource(strings = {TSL_ACA, TSL_PID, TSL_WALLET})
    @DisplayName("then getTrustlistAsLoTE returns plain LoTE for requested trustlist with ListAndSchemeInformation and TrustedEntitiesList with content")
    void getTrustlistAsLoTE(String trustlist) {
        when(generatorService.generateTrustlist(eq(trustlist))).thenReturn(createLoTETrustlist());
        LoTE loTE = signed602TrustlistService.getTrustlistAsLoTE(trustlist);
        assertNotNull(loTE);
        assertNotNull(loTE.getListAndSchemeInformation());
        assertNotNull(loTE.getListAndSchemeInformation().getLoTEType());
        assertNotNull(loTE.getTrustedEntitiesList());
        verify(generatorService, times(1)).generateTrustlist(eq(trustlist));
    }

    private Map<String, Trustlist> createTrustlistMapFromProperties() {
        Map<String, Trustlist> trustlistMap = new HashMap<>();
        Trustlist tlAca = new Trustlist("path-a", keystoreNameAca, null, null);
        Trustlist tlPid = new Trustlist("path-p", keystoreNamePid, null, null);
        trustlistMap.put(TSL_ACA, tlAca);
        trustlistMap.put(TSL_PID, tlPid);
        return trustlistMap;
    }

    private static void verifySignedTrustlistResult(String signedTrustlist) throws ParseException {
        assertNotNull(signedTrustlist);
        JWSObject jwsObject = JWSObject.parse(signedTrustlist);
        assertNotNull(jwsObject);
        String body = jwsObject.getPayload().toString();
        assertTrue(body.contains(DIGDIR)); // just check some expected content in payload
    }


    private static @NonNull String getJwtWithValidHeaderAndPayload(LoTE loTETrustlist) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .customParam("iat", Instant.now().getEpochSecond())
                .build();

        Payload payload = new Payload(createJsonFromLoTE(loTETrustlist));
        return header.toBase64URL().toString() + "." + payload.toBase64URL().toString() + ".signature";
    }
}

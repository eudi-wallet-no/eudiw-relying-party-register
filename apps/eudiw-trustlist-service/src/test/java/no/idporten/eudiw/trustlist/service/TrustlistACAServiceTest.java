package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.TestDataGenerator;
import no.idporten.eudiw.trustlist.config.TrustlistACAProperties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When get 602 trustlist")
class TrustlistACAServiceTest {

    @Mock
    private TrustlistACAGeneratorService generatorService;

    @Mock
    private JsonSignerService jsonSignerService;

    @Mock
    private TrustlistACAProperties acaProperties;

    @InjectMocks
    private TrustlistACAService signed602TrustlistService;

    private static final String keystoreName = "signing-602";

    @BeforeEach
    public void setup() {
        when(generatorService.generateTrustlistACA()).thenReturn(TestDataGenerator.createLoTETrustlist());
    }

    @Test
    @DisplayName("as LoTE then return LoTE with content")
    void getACATrustlistAsLoTE() {
        LoTE acaTrustlistAsLoTE = signed602TrustlistService.getACATrustlistAsLoTE();
        assertNotNull(acaTrustlistAsLoTE);
        verify(generatorService, only()).generateTrustlistACA();
    }

    @Test
    @DisplayName("as signed json then return String with content")
    void getSignedACATrustlist() {
        String signedJson = "signedJson";
        when(acaProperties.keystore()).thenReturn(keystoreName);
        when(jsonSignerService.signedTrustlist(any(LoTE.class), eq(keystoreName))).thenReturn(signedJson);
        String signedACATrustlist = signed602TrustlistService.signedACAJson();
        assertNotNull(signedACATrustlist);
        assertEquals(signedJson, signedACATrustlist);
        verify(jsonSignerService, only()).signedTrustlist(any(LoTE.class), anyString());
    }
}

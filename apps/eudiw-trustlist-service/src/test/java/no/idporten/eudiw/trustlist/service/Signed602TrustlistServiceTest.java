package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.TestDataGenerator;
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
class Signed602TrustlistServiceTest {

    @Mock
    private TrustlistACAGeneratorService generatorService;

    @Mock
    private JsonSignerService jsonSignerService;

    @InjectMocks
    private Signed602TrustlistService signed602TrustlistService;

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
        when(jsonSignerService.signedTrustlist(any(LoTE.class))).thenReturn(signedJson);
        String signedACATrustlist = signed602TrustlistService.getSignedACATrustlist();
        assertNotNull(signedACATrustlist);
        assertEquals(signedJson, signedACATrustlist);
        verify(jsonSignerService, only()).signedTrustlist(any(LoTE.class));
    }
}

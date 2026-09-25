package no.idporten.eudiw.trustlist.web;


import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.trustlist.TestDataGenerator;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.service.Trustlist602Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@DisplayName("When using a 602 controller")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class Trustlist602ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trustlist602Properties properties;

    @MockitoBean
    private Trustlist602Service service;

    @ParameterizedTest
    @ValueSource(strings = {TSL_ACA, TSL_PID, TSL_WALLET})
    @DisplayName("When GETTING LoTE then return as JSON with ListAndSchemeInformation with content")
    void testAcaControllerReturnsLoTE(String trustlist) throws Exception {

        LoTE lote = TestDataGenerator.createLoTETrustlist();
        when(service.getTrustlistAsLoTE(trustlist)).thenReturn(lote);

        mockMvc.perform(get(properties.tsl602().get(trustlist).path()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.LoTE.ListAndSchemeInformation.LoTEType").value(lote.getListAndSchemeInformation().getLoTEType().toString()))
                .andExpect(jsonPath("$.LoTE.ListAndSchemeInformation.SchemeTerritory").value(lote.getListAndSchemeInformation().getSchemeTerritory()));

        verify(service, times(1)).getTrustlistAsLoTE(eq(trustlist));

    }

    @ParameterizedTest
    @ValueSource(strings = {TSL_ACA, TSL_PID, TSL_WALLET})
    @DisplayName("When GETTING signed trustlist then return JWS with content")
    void testACAControllerReturnsSignedTrustlist(String trustlist) throws Exception {

        when(service.getSignedTrustlist(trustlist)).thenReturn("test");

        mockMvc.perform(get(properties.tsl602().get(trustlist).path() + ".jws"))
                .andExpect(content().contentType(TrustlistMediaTypes.APPLICATION_JOSE_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("test"))
                .andExpect(header().exists("Last-Modified"));
    }

    @ParameterizedTest
    @ValueSource(strings = {TSL_ACA, TSL_PID, TSL_WALLET})
    @DisplayName("When GETTING signed trustlist for mediatype='application/vnd.lote+json' then return JWS with content and same media type")
    void testACAControllerReturnsSignedTrustlistForCustomMediatype(String trustlist) throws Exception {

        when(service.getSignedTrustlist(trustlist)).thenReturn("test");

        mockMvc.perform(get(properties.tsl602().get(trustlist).path() + ".jws").accept(TrustlistMediaTypes.APPLICATION_VND_LOTE_JSON))
                .andExpect(content().contentType(TrustlistMediaTypes.APPLICATION_VND_LOTE_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("test"))
                .andExpect(header().exists("Last-Modified"));
    }
}
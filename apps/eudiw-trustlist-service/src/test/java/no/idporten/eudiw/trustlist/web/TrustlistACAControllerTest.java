package no.idporten.eudiw.trustlist.web;


import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.trustlist.TestDataGenerator;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.service.Trustlist602Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.TSL_ACA;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@DisplayName("When using ACA controller")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class TrustlistACAControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trustlist602Properties properties;

    @MockitoBean
    private Trustlist602Service service;

    @Test
    @DisplayName("When GETTING LoTE then return as JSON with ListAndSchemeInformation with content")
    void testAcaControllerReturnsLoTE() throws Exception {

        LoTE lote = TestDataGenerator.createLoTETrustlist();
        when(service.getTrustlistAsLoTE(TSL_ACA)).thenReturn(lote);

        mockMvc.perform(get(properties.getAcaTrustlist().path()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.ListAndSchemeInformation.LoTEType").value(lote.getListAndSchemeInformation().getLoTEType().toString()))
                .andExpect(jsonPath("$.ListAndSchemeInformation.SchemeTerritory").value(lote.getListAndSchemeInformation().getSchemeTerritory()));

        verify(service, times(1)).getTrustlistAsLoTE(TSL_ACA);

    }

    @Test
    @DisplayName("When GETTING signed trustlist then return JWS with content")
    void testACAControllerReturnsSignedTrustlist() throws Exception {

        when(service.getSignedTrustlist(TSL_ACA)).thenReturn("test");

        mockMvc.perform(get(properties.getAcaTrustlist().path() + ".jws"))
                .andExpect(content().contentType("application/jose+json"))
                .andExpect(status().isOk())
                .andExpect(content().string("test"));
    }
}
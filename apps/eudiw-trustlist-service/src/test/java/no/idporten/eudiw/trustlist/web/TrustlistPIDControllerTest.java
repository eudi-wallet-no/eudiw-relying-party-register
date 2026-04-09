package no.idporten.eudiw.trustlist.web;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.trustlist.TestDataGenerator;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.service.TrustlistPIDService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@DisplayName("When using PID controller")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class TrustlistPIDControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trustlist602Properties properties;

    @MockitoBean
    private TrustlistPIDService service;

    @Test
    @DisplayName("When GETTING PID LoTE then return as JSON with ListAndSchemeInformation with content")
    void testPidControllerReturnsLoTE() throws Exception {

        LoTE lote = TestDataGenerator.createLoTETrustlist();
        when(service.getPIDTrustlistAsLoTE()).thenReturn(lote);

        mockMvc.perform(get(properties.getPidTrustlist().path()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.ListAndSchemeInformation.LoTEType").value(lote.getListAndSchemeInformation().getLoTEType().toString()))
                .andExpect(jsonPath("$.ListAndSchemeInformation.SchemeTerritory").value(lote.getListAndSchemeInformation().getSchemeTerritory()));

        verify(service, times(1)).getPIDTrustlistAsLoTE();

    }

    @Test
    @DisplayName("When GETTING signed PID trustlist then return JWS payload")
    void testPidControllerReturnsSignedTrustlist() throws Exception {

        String signedTrustlist = "eyJhbGciOiJFUzI1NiJ9.eyJ0cnVzdGxpc3QiOiJwaWQifQ.signature";
        when(service.getSignedPidTrustlist()).thenReturn(signedTrustlist);

        mockMvc.perform(get(properties.getPidTrustlist().path() + ".jws"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/jose+json"))
                .andExpect(content().string(signedTrustlist))
                .andExpect(header().exists("Last-Modified"));

        verify(service, times(1)).getSignedPidTrustlist();
    }
}

package no.idporten.eudiw.trustlist.web;


import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static no.idporten.eudiw.trustlist.config.Trustlist602Properties.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DisplayName("When using 602 controllers")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class Trustlist602JwtGeneratedOnceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trustlist602Properties properties;

    @ParameterizedTest
    @ValueSource(strings = {TSL_ACA, TSL_PID, TSL_WALLET})
    @DisplayName("When GETTING signed trustlist then the same list is return on multiple requests")
    void verifyListIsOnlyGeneratedOnceOnStartup(String trustlist) throws Exception {

        String uri = properties.tsl602().get(trustlist).path() + ".jws";
        String contentType = "application/jose+json";

        // Run 1
        MvcResult mvcResult1 = mockMvc.perform(get(uri))
                .andExpect(content().contentType(contentType))
                .andExpect(status().isOk())
                .andReturn();
        String jws1 = mvcResult1.getResponse().getContentAsString();
        assertNotNull(jws1);
        String[] split = jws1.split("\\.");
        assertEquals(3, split.length);
        String json1 = new String(Base64.getUrlDecoder().decode(split[1]));
        assertNotNull(json1);

        // Run 2
        MvcResult mvcResult2 = mockMvc.perform(get(uri))
                .andExpect(content().contentType(contentType))
                .andExpect(status().isOk())
                .andReturn();
        String jws2 = mvcResult2.getResponse().getContentAsString();
        assertNotNull(jws2);
        String[] split2 = jws2.split("\\.");
        assertEquals(3, split2.length);
        String json2 = new String(Base64.getUrlDecoder().decode(split2[1]));
        assertNotNull(json2);

        // Same result
        assertEquals(jws1, jws2);
        assertEquals(json1, json2);
    }
}
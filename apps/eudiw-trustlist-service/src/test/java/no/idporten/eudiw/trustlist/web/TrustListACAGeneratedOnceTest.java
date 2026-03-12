package no.idporten.eudiw.trustlist.web;


import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.trustlist.config.TrustListACAProperties;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DisplayName("When using ACA controller")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class TrustListACAGeneratedOnceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrustListACAProperties properties;

    @Test
    @DisplayName("When GETTING signed trustlist then the same list is return on multiple requests")
    void verifyListIsOnlyGeneratedOnceOnStartup() throws Exception {

        String uri = properties.path() + ".jws";
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
        String json1 = new String(Base64.decode(split[1]));
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
        String json2 = new String(Base64.decode(split2[1]));
        assertNotNull(json2);

        // Same result
        assertEquals(jws1, jws2);
        assertEquals(json1, json2);
    }
}
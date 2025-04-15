package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.service.XMLSignerService;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("When downloading trust status lists")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class TrustListControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private XMLSignerService xmlSignerService;

    @DisplayName("then the TSL is signed")
    @Test
    void testSignedTSL() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/access_tsl.xts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.etsi.tsl+xml;charset=UTF-8"))
                .andReturn();
        String xml = mvcResult.getResponse().getContentAsString();
        Document document = XMLUtils.parseXml(xml);
        assertTrue(xmlSignerService.validateEnvelopedSignature(document));
    }

}

package no.idporten.eudiw.trustlist.web;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.xml.utils.DomUtils;
import eu.europa.esig.trustedlist.TrustedListUtils;
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

import javax.xml.transform.dom.DOMSource;
import java.util.List;

import static no.idporten.eudiw.trustlist.web.TrustListController.PATH_ACCESS_TRUSTLIST_SHA;
import static no.idporten.eudiw.trustlist.web.TrustListController.PATH_ACCESS_TRUSTLIST_XTSL;
import static org.junit.jupiter.api.Assertions.*;
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
        MvcResult mvcResult = mockMvc.perform(get(PATH_ACCESS_TRUSTLIST_XTSL))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.etsi.tsl+xml;charset=UTF-8"))
                .andReturn();
        String xml = mvcResult.getResponse().getContentAsString();
        Document document = XMLUtils.parseXml(xml);
        assertTrue(xmlSignerService.validateEnvelopedSignature(document));
    }


    @DisplayName("then the TSL is signed and valid")
    @Test
    void testSignedTSLisValid() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(PATH_ACCESS_TRUSTLIST_XTSL))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.etsi.tsl+xml;charset=UTF-8"))
                .andReturn();

        byte[] xml = mvcResult.getResponse().getContentAsByteArray();
        DSSDocument dssDoc = new InMemoryDocument(xml);
        Document tlDocDom = DomUtils.buildDOM(dssDoc);
        List<String> errors = TrustedListUtils.getInstance().validateAgainstXSD(new DOMSource(tlDocDom));
        assertTrue(errors.isEmpty(), "Signed XML contains errors");

    }

    @DisplayName("then the sha-256 hash of the TSL is of valid length")
    @Test
    void testSha256OfTrustlistIsValid() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(PATH_ACCESS_TRUSTLIST_SHA))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andReturn();

        String sha256 = mvcResult.getResponse().getContentAsString();
        assertNotNull(sha256);
        assertEquals(64, sha256.length(), "SHA-256 hash should be 64 characters long (32 bytes in hex representation)");
    }

}

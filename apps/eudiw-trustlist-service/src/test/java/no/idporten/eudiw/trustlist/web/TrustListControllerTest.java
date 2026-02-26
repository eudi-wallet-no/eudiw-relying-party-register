package no.idporten.eudiw.trustlist.web;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.xml.utils.DomUtils;
import eu.europa.esig.trustedlist.TrustedListUtils;
import no.idporten.eudiw.trustlist.config.TrustlistServiceProperties;
import no.idporten.eudiw.trustlist.service.XMLSignerService;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@DisplayName("When downloading trust status lists")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class TrustListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private XMLSignerService xmlSignerService;

    @Autowired
    private TrustlistServiceProperties properties;

    private String getPathXtsl() {
        return properties.getTsl612().trustlistPathXtsl();
    }

    private String getPathSha2() {
        return properties.getTsl612().trustlistPathSha2();
    }
    private String getPathDefault() {
        return properties.getTsl612().trustlistPath();
    }

    @DisplayName("then the TSL is signed")
    @Test
    void testSignedTSL() throws Exception {

        MvcResult mvcResult = mockMvc.perform(get(getPathXtsl()))
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
        MvcResult mvcResult = mockMvc.perform(get(getPathXtsl()))
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
        MvcResult mvcResult = mockMvc.perform(get(getPathSha2()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andReturn();

        String sha256 = mvcResult.getResponse().getContentAsString();
        assertNotNull(sha256);
        assertEquals(64, sha256.length(), "SHA-256 hash should be 64 characters long (32 bytes in hex representation)");
    }

    @Test
    @DisplayName("eTag is returned as response header in sha2 and xtsl endpoints")
    public void eTagReturnedAsResponseHeader() throws Exception {
        MvcResult shaResult = mockMvc.perform(get(getPathSha2()))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("Last-Modified"))
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andReturn();

        assertNotNull(shaResult.getResponse().getHeader("ETag"));

        MvcResult trustlistResult = mockMvc.perform(get(getPathXtsl()))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("Last-Modified"))
                .andExpect(content().contentType("application/vnd.etsi.tsl+xml;charset=UTF-8"))
                .andReturn();

        assertNotNull(trustlistResult.getResponse().getHeader("ETag"));

        assertNotEquals(shaResult.getResponse().getHeader("ETag"), trustlistResult.getResponse().getHeader("ETag"));

        // this might be removed in the future, but for now test that ETag is not added here
        mockMvc.perform(get(getPathDefault()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("ETag"))
                .andExpect(header().exists("Last-Modified"))
                .andReturn();

    }

    @Test
    @DisplayName("when retrieving the resource with ETag, then HTTP STATUS=not modified is returned if the resource has not changed")
    public void verifyNoContentIsReturnedOnSecondCallWithIfNoneMatch() throws Exception {
        MvcResult mvcResult1 = mockMvc.perform(get(getPathSha2()))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("Last-Modified"))
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andReturn();

        String eTag1 = mvcResult1.getResponse().getHeader("ETag");
        assertNotNull(eTag1);
        assertTrue(mvcResult1.getResponse().getContentLength() > 0);

        MvcResult mvcResult2 = mockMvc.perform(get(getPathSha2()).header("If-None-Match", eTag1))
                .andExpect(status().isNotModified())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("Last-Modified"))
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andReturn();

        assertEquals(0, mvcResult2.getResponse().getContentLength());
        String eTag2 = mvcResult2.getResponse().getHeader("ETag");
        assertEquals(eTag1, eTag2);
    }

}

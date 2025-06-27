package no.idporten.eudiw.trustlist.web;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.xml.utils.DomUtils;
import eu.europa.esig.trustedlist.TrustedListUtils;
import no.idporten.eudiw.trustlist.service.XMLSignerService;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static no.idporten.eudiw.trustlist.web.TrustListController.PATH_ACCESS_TRUSTLIST;
import static no.idporten.eudiw.trustlist.xml.XMLUtils.parseTrustlist;
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
        MvcResult mvcResult = mockMvc.perform(get(PATH_ACCESS_TRUSTLIST))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.etsi.tsl+xml;charset=UTF-8"))
                .andReturn();
        String xml = mvcResult.getResponse().getContentAsString();
        Document document = XMLUtils.parseXml(xml);
        assertTrue(xmlSignerService.validateEnvelopedSignature(document));
    }

    @Test
    @DisplayName("then TSL signed but without content is not valid")
    void emptyTrustlistIsNotValid() throws Exception {
        TrustServiceStatusList trustServiceStatusList = new TrustServiceStatusList();

        Document d = parseTrustlist(trustServiceStatusList);
        Document signedXml = xmlSignerService.createEnvelopedSignature(d);
        assertNotNull(signedXml, "Signed XML should not be null");

        byte[] array = getBytesOfDocument(signedXml);
        // Additionally, the TL can be validated against the XSD schema
        DSSDocument dssDoc = new InMemoryDocument(array);
        Document tlDocDom = DomUtils.buildDOM(dssDoc);
        List<String> errors = TrustedListUtils.getInstance().validateAgainstXSD(new DOMSource(tlDocDom));
        assertFalse(errors.isEmpty(), "Signed XML does not contains errors");
    }

    @DisplayName("then the TSL is signed and valid")
    @Test
    void testSignedTSLisValid() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(PATH_ACCESS_TRUSTLIST))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.etsi.tsl+xml;charset=UTF-8"))
                .andReturn();

        byte[] xml = mvcResult.getResponse().getContentAsByteArray();
        DSSDocument dssDoc = new InMemoryDocument(xml);
        Document tlDocDom = DomUtils.buildDOM(dssDoc);
        List<String> errors = TrustedListUtils.getInstance().validateAgainstXSD(new DOMSource(tlDocDom));
        assertTrue(errors.isEmpty(), "Signed XML contains errors");

    }

    private static byte[] getBytesOfDocument(Document signedXml) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(signedXml);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(bos);
        transformer.transform(source, result);
        return bos.toByteArray();
    }

}

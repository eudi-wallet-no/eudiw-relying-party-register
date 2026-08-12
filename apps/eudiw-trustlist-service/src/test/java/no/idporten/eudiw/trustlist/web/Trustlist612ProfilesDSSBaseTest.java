package no.idporten.eudiw.trustlist.web;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.xml.utils.DomUtils;
import eu.europa.esig.trustedlist.TrustedListUtils;
import no.idporten.eudiw.trustlist.config.Trustlist612Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles({"signing"})
public abstract class Trustlist612ProfilesDSSBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trustlist612Properties properties;

    private String getPathXtsl() {
        return properties.trustlistPathXtsl();
    }

    @DisplayName("then the TSL is signed and valid")
    @Test
    void testSignedTSLisValid() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(getPathXtsl()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.etsi.tsl+xml;charset=UTF-8"))
                .andReturn();

        List<String> errors = validateByDSSLib(mvcResult.getResponse());
        assertTrue(errors.isEmpty(), "Signed XML contains errors: " + errors);
    }

    protected static List<String> validateByDSSLib(MockHttpServletResponse response) {
        byte[] xml = response.getContentAsByteArray();
        DSSDocument dssDoc = new InMemoryDocument(xml);
        Document tlDocDom = DomUtils.buildDOM(dssDoc);
        return TrustedListUtils.getInstance().validateAgainstXSD(new DOMSource(tlDocDom));
    }

}



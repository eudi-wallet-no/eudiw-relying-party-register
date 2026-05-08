package no.idporten.eudiw.trustlist.web;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.xml.utils.DomUtils;
import eu.europa.esig.trustedlist.TrustedListUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// This test is an Integration test, thus it is disabled by default and should be run manually when needed.
//
// It validates that the TSLs provided by Nkom and eIDAS2Sandkasse TEST are signed and valid according to the XSD schema.
//@Disabled
public class Trustlist612NkomIntTest {

    @DisplayName("When validate online trustlist then the TSL is signed and according to XSD schema")
    @ParameterizedTest
    @ValueSource(strings = {"https://nkom.no/files/TL/NO_TL.xml", "https://tillitsliste.test.eidas2sandkasse.net/no_eidas2sandkasse_test_tsl.xtsl"})
    void validateSignedTLAgainstXsd(String trustlistUrl) {
        RestTestClient client = RestTestClient.bindToServer().baseUrl(trustlistUrl).build();
        EntityExchangeResult<byte[]> response = client.get().exchange()
                .expectStatus().isOk()
                .returnResult(byte[].class);

        assertNotNull(response.getResponseBody(), "Response body from " + trustlistUrl + " should not be null");
        List<String> errors = validateByDSSLib(response.getResponseBody());
        assertTrue(errors.isEmpty(), "Signed Trustlist 612 XML from " + trustlistUrl + " contains errors: " + errors);
    }

    private static List<String> validateByDSSLib(byte[] xml) {
        DSSDocument dssDoc = new InMemoryDocument(xml);
        Document tlDocDom = DomUtils.buildDOM(dssDoc);
        return TrustedListUtils.getInstance().validateAgainstXSD(new DOMSource(tlDocDom));
    }

}



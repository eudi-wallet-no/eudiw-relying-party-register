package no.idporten.eudiw.trustlist.service;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.xml.utils.DomUtils;
import eu.europa.esig.trustedlist.TrustedListUtils;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static no.idporten.eudiw.trustlist.xml.XMLUtils.parseTrustlist;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("junit")
@SpringBootTest
@DisplayName("When signed trustlist is tested by DSS lib")
public class SignedTrustListDSSTest {

    @Autowired
    private XMLSignerService xmlSignerService;

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

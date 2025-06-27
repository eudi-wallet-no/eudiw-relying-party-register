package no.idporten.eudiw.trustlist.service;

import eu.europa.esig.dss.jaxb.object.Message;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.policy.ValidationPolicy;
import eu.europa.esig.dss.policy.EtsiValidationPolicy;
import eu.europa.esig.dss.policy.ValidationPolicyFacade;
import eu.europa.esig.dss.policy.jaxb.ConstraintsParameters;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.xml.bind.JAXBException;
import no.idporten.eudiw.trustlist.xml.XMLUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("Document of xml is created and signature of xml is")
class XMLSignerServiceTest {

    @Autowired
    XMLSignerService xmlSignerService;

    @Test
    @DisplayName("created and valid")
    void testCreateEnvelopedSignature() throws Exception {
        String xml = "<root><element>Test</element></root>";
        Document d = XMLUtils.parseXml(xml);
        Document signedXml = xmlSignerService.createEnvelopedSignature(d);
        assertNotNull(signedXml, "Signed XML should not be null");
    }

    // Mostly a simplification of https://ec.europa.eu/digital-building-blocks/code/projects/ESIG/repos/dss-demos/browse/dss-esig-validation-tests/src/test/java/eu/europa/esig/dss/validation/EsigValidationTest.java
    @Test
    @Disabled
    @DisplayName("created and validated by DDS lib")
    void testCreateEnvelopedSignatureWithDDSLib() throws Exception {
        String xml = "<root><element>Test</element></root>";
        Document d = XMLUtils.parseXml(xml);
        Document signedDoc = xmlSignerService.createEnvelopedSignature(d);
        String signedXml = XMLUtils.formatXml(signedDoc);
        DSSDocument dssDoc = new InMemoryDocument(signedXml.getBytes(StandardCharsets.UTF_8));

        // Setup the validation for the DSS library
        ValidationPolicy validationPolicy = setupValidationPolicy("src/test/resources/constraint.xml");
        CertificateVerifier certificateVerifier = setupCertificateVerifier();

        SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(dssDoc);
        validator.setCertificateVerifier(certificateVerifier);

        // Validate the signed document using the DSS library
        Reports reports = validator.validateDocument(validationPolicy);
        assertNotNull(reports, "Reports should not be null");

        SimpleReport simpleReport = reports.getSimpleReport();
        String obtainedResult = simpleReport.getSignatureQualification(simpleReport.getFirstSignatureId()).getReadable();
        assertNotNull(obtainedResult, "Signature qualification should not be null"); // Do not know, its N/A now... Delete test?

//        reports.print();
        System.out.println("Indication: " + simpleReport.getIndication(simpleReport.getFirstSignatureId()));
        System.out.println("SubIndication: " + simpleReport.getSubIndication(simpleReport.getFirstSignatureId()));
        System.out.println("AdESValidationErrors should be null: " + toString(simpleReport.getAdESValidationErrors(simpleReport.getFirstSignatureId())));
        System.out.println("AdESValidationWarnings: " + toString(simpleReport.getAdESValidationWarnings(simpleReport.getFirstSignatureId())));
        System.out.println("QualificationErrors should be null: " + toString(simpleReport.getQualificationErrors(simpleReport.getFirstSignatureId())));
        System.out.println("QualificationWarnings: " + toString(simpleReport.getQualificationWarnings(simpleReport.getFirstSignatureId())));

        assertNull(simpleReport.getAdESValidationErrors(simpleReport.getFirstSignatureId()), "AdESValidationErrors should be null: " + toString(simpleReport.getAdESValidationErrors(simpleReport.getFirstSignatureId())));
        assertNull(simpleReport.getQualificationErrors(simpleReport.getFirstSignatureId()), "QualificationErrors should be null: " + toString(simpleReport.getQualificationErrors(simpleReport.getFirstSignatureId())));

    }

    @NotNull
    private static ValidationPolicy setupValidationPolicy(final String policyConstraintXml) throws JAXBException, XMLStreamException, IOException, SAXException {
        ValidationPolicyFacade policyFacade = ValidationPolicyFacade.newFacade();
        ConstraintsParameters constraints = policyFacade.unmarshall(new File(policyConstraintXml));
        return new EtsiValidationPolicy(constraints);
    }

    @NotNull
    private CertificateVerifier setupCertificateVerifier() {
        //        DataLoader dataLoader = new CommonsDataLoader();
//        File fileCacheDirectory = new File("target/cache");
//        FileCacheDataLoader fileCacheDataLoader = new FileCacheDataLoader();
//        fileCacheDataLoader.setDataLoader(dataLoader);
//        fileCacheDataLoader.setCacheExpirationTime(-1);
//        fileCacheDataLoader.setFileCacheDirectory(fileCacheDirectory);

        CertificateVerifier certificateVerifier = new CommonCertificateVerifier();
        TrustedListsCertificateSource trustedListsCertificateSource = new TrustedListsCertificateSource();
        certificateVerifier.setTrustedCertSources(trustedListsCertificateSource);
//        certificateVerifier.setAIASource(new DefaultAIASource(fileCacheDataLoader));
//        certificateVerifier.setCrlSource(new OnlineCRLSource(fileCacheDataLoader));
//        certificateVerifier.setOcspSource(new OnlineOCSPSource(fileCacheDataLoader));
        return certificateVerifier;
    }

//    @Test
//    @DisplayName("Test internal method toString with non-empty list")
//    void testToStringWithNull() {
//        String result = toString(null);
//        assertNull(result, "toString should return null for null input");
//    }
//
//    @Test
//    @DisplayName("Test internal method toString with non-empty list")
//    void testToStringWithEmptyList() {
//        String result = toString(Collections.emptyList());
//        assertNull(result, "toString should return null for empty list");
//    }

    private String toString(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        messages.forEach(message -> sb.append(message.getKey()).append(": ").append(message.getValue()).append(";"));
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

}
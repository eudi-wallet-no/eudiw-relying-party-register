package no.idporten.eudiw.trustlist.service;

import jakarta.xml.bind.JAXBException;
import no.idporten.eudiw.trustlist.web.ApplicationException;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static no.idporten.eudiw.trustlist.xml.XMLUtils.parseTrustlist;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SignedTrustListServiceTest")
@ExtendWith(SpringExtension.class)
class SignedTrustListServiceTest {

    @Mock
    private TrustListGeneratorService trustListGeneratorService;

    @Mock
    private XMLSignerService xmlSignerService;

    @InjectMocks
    private SignedTrustListService signedTrustListService;

    @Test
    @DisplayName("getTrustlist returns a non-null signed trustlist")
    void getTrustlist() throws Exception {
        TrustServiceStatusList trustServiceStatusList = createTrustServiceStatusList();
        when(trustListGeneratorService.generateTrustServiceStatusList()).thenReturn(trustServiceStatusList);
        when(xmlSignerService.createEnvelopedSignature(any(Document.class))).thenReturn(createSignedTrustlist());
        String trustlist = signedTrustListService.getTrustlist();
        System.out.println(trustlist);
        assertNotNull(trustlist);
        verify(trustListGeneratorService).generateTrustServiceStatusList();
        verify(xmlSignerService).createEnvelopedSignature(any(Document.class));
    }

    @Test
    @DisplayName("getTrustlist throws SigningException when signing fails")
    void getTrustlistFailesWhenSigningFails() {
        TrustServiceStatusList trustServiceStatusList = createTrustServiceStatusList();
        when(trustListGeneratorService.generateTrustServiceStatusList()).thenReturn(trustServiceStatusList);
        when(xmlSignerService.createEnvelopedSignature(any(Document.class))).thenThrow(SigningException.class);
        assertThrows(SigningException.class , ()-> signedTrustListService.getTrustlist());
        verify(trustListGeneratorService).generateTrustServiceStatusList();
        verify(xmlSignerService).createEnvelopedSignature(any(Document.class));
    }

    @Test
    @DisplayName("getTrustlist throws ApplicationException when generate Trustlist fails")
    void getTrustlistFailesWhenTrustlistGenerationFails() {
        when(trustListGeneratorService.generateTrustServiceStatusList()).thenThrow(ApplicationException.class);
        assertThrows(ApplicationException.class , ()-> signedTrustListService.getTrustlist());
        verify(trustListGeneratorService).generateTrustServiceStatusList();
        verify(xmlSignerService, never()).createEnvelopedSignature(any(Document.class));
    }

    private static Document createSignedTrustlist() throws JAXBException {
        Document trustlist = parseTrustlist(createTrustServiceStatusList());
        Element signature = trustlist.createElement("Signature");
        signature.setAttribute("fakeSignature", "but I do not care");
        trustlist.getDocumentElement().appendChild(signature);
        return trustlist;
    }

    @NotNull
    private static TrustServiceStatusList createTrustServiceStatusList() {
        TrustServiceStatusList trustServiceStatusList = new TrustServiceStatusList();
        trustServiceStatusList.setId("trustlist-id");
        return trustServiceStatusList;
    }

    @Test
    @DisplayName("getSha2 generate a valid sha-256 hash of the trustlist")
    void getSha2() throws Exception {
        TrustServiceStatusList trustServiceStatusList = createTrustServiceStatusList();
        when(trustListGeneratorService.generateTrustServiceStatusList()).thenReturn(trustServiceStatusList);
        when(xmlSignerService.createEnvelopedSignature(any(Document.class))).thenReturn(createSignedTrustlist());
        String sha256 = signedTrustListService.getSha2();
        System.out.println(sha256);
        assertNotNull(sha256);
        assertEquals(signedTrustListService.generateShaOfString(signedTrustListService.getTrustlist()), sha256);
        verify(trustListGeneratorService).generateTrustServiceStatusList();
        verify(xmlSignerService).createEnvelopedSignature(any(Document.class));
    }

    @Test
    @DisplayName("generateShaOfString returns a non-null SHA-256 hash of the input string")
    void generateShaOfString() {
        String value = "I need this value hashed";
        String sha256 = signedTrustListService.generateShaOfString(value);
        System.out.println(sha256);
        assertNotNull(sha256, "SHA-256 hash should not be null");
        assertTrue(sha256.matches("^[a-fA-F0-9]{64}$"), "SHA-256 hash should be 64 hex characters");
    }


}
package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.exception.ApplicationException;
import org.etsi.uri._02231.v2_.TrustServiceStatusList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import static no.idporten.eudiw.trustlist.TestDataGenerator.createSignedTrustlist;
import static no.idporten.eudiw.trustlist.TestDataGenerator.createTrustServiceStatusList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SignedTrustListServiceTest")
@ExtendWith(MockitoExtension.class)
class SignedTrustlistServiceTest {

    @Mock
    private TrustlistGeneratorService trustListGeneratorService;

    @Mock
    private XMLSignerService xmlSignerService;

    @InjectMocks
    private SignedTrustlistService signedTrustListService;

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
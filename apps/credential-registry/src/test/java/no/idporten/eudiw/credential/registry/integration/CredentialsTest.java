package no.idporten.eudiw.credential.registry.integration;


import no.idporten.eudiw.credential.registry.integration.model.CredentialIssuer;
import no.idporten.eudiw.credential.registry.response.CredentialRegisterService;
import no.idporten.eudiw.credential.registry.response.model.Credentials;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;

@DisplayName("credentials testing")
@SpringBootTest
@ActiveProfiles("junit")
public class CredentialsTest {

    private static final Logger log = LoggerFactory.getLogger(CredentialsTest.class);
    @MockitoBean
    private CredentialRegisterService credentialRegisterService;

    private DummyMetadataRetriever mockRetriever;


    @Autowired
    public CredentialsTest(DummyMetadataRetriever credentialIssuerMetadataRetriever) {
        this.mockRetriever = credentialIssuerMetadataRetriever;
    }


    @DisplayName("When registering credentials")
    @Test
    void whenReformattingTheData() {
        this.credentialRegisterService = new CredentialRegisterService(mockRetriever);
        credentialRegisterService.setResponse();
        Credentials credentials = credentialRegisterService.getCredentials();

        assertAll(
                () -> assertEquals("mock_utsteder", credentials.credentials().get(0).credentialIssuer()),
                () -> assertEquals("sd+jwt-vc", credentials.credentials().get(0).format()),
                () -> assertEquals("mdoc", credentials.credentials().get(1).format()),
                () -> assertEquals("jwt_vc_json", credentials.credentials().get(2).format())
        );
    }
    @DisplayName("When issuer uri is empty")
    @Test
    void whenIssuerUriIsEmpty() {
        URI uri = URI.create("");
        CredentialIssuer issuer = mockRetriever.fetchCredentialIssuerFromMetadataRequest(uri);
        assertNull(issuer);
    }


// TODO: se på de siste testene seinere: Må mocke server/url for å få enabla de.
//    @DisplayName("When issuer URL is not https")
//    @Test
//    void whenIssuerUrlIsNotHttps() {
//        URI uri = URI.create("http://localhost:8080/");
//        CredentialIssuer issuer = mockRetriever.fetchCredentialIssuerFromMetadataRequest(uri);
//
//        assertNull(issuer);
//    }
//
//    @DisplayName("When issuer uri is the way it is supposed to be, it is built correct with well known")
//    @Test
//    void whenIssuerUriIsTheWayItIsSupposedToBe() {
//        URI uri = URI.create("https://utsteder.test.eidas2sandkasse.net");
//        URI result = mockRetriever.buildWellKnown(uri);
//        assertEquals("https://utsteder.test.eidas2sandkasse.net/.well-known/openid-credential-issuer",  result.toString());
//    }
//
//    @DisplayName("When trailing slash case")
//    @Test
//    void whenTrailingSlashCase() {
//        URI uri = URI.create("https://utsteder.test.eidas2sandkasse.net/");
//        URI result = mockRetriever.buildWellKnown(uri);
//        assertEquals("https://utsteder.test.eidas2sandkasse.net/.well-known/openid-credential-issuer", result.toString());
//    }
}

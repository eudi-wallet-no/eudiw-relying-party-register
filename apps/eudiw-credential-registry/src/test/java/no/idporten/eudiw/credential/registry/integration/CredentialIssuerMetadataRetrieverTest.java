package no.idporten.eudiw.credential.registry.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@DisplayName("credentials issuer metadata retriever testing")
@SpringBootTest
@ActiveProfiles("junit")
public class CredentialIssuerMetadataRetrieverTest {


    private CredentialIssuerMetadataRetriever retriever;


    @Autowired
    public CredentialIssuerMetadataRetrieverTest(CredentialIssuerMetadataRetriever credentialIssuerMetadataRetriever) {
        this.retriever = credentialIssuerMetadataRetriever;
    }


    @DisplayName("When issuer URL is not https")
    @Test
    void whenIssuerUrlIsNotHttps() {
        URI uri = URI.create("http://localhost:8080/");
        boolean answer = retriever.isHttps(uri);
        assertFalse(answer);
    }


    @DisplayName("When issuer uri is the way it is supposed to be, it is built correct with well known")
    @Test
    void whenIssuerUriIsTheWayItIsSupposedToBe() {
        URI uri = URI.create("https://utsteder.test.eidas2sandkasse.net");
        URI result = retriever.buildWellKnown(uri);
        assertEquals("https://utsteder.test.eidas2sandkasse.net/.well-known/openid-credential-issuer",  result.toString());
    }

    @DisplayName("When trailing slash case")
    @Test
    void whenTrailingSlashCase() {
        URI uri = URI.create("https://utsteder.test.eidas2sandkasse.net/");
        URI result = retriever.buildWellKnown(uri);
        assertEquals("https://utsteder.test.eidas2sandkasse.net/.well-known/openid-credential-issuer", result.toString());
    }

}

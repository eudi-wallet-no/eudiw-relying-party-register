package no.idporten.eudiw.credential.registry.integration;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrregIssuerMetadataTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        dc+sd-jwt | ["name"] | 2
        unsupported | ["name"] | 1
        dc+sd-jwt | "not an array" | 1
        """)
    void validCredentialsSurviveInvalidSibling(String format, String path, int expectedCount) {
        String metadata = """
            {
              "credential_issuer": "https://issuer.example",
              "credential_configurations_supported": {
                "EBWOID": {"format": "dc+sd-jwt", "vct": "ebwoid"},
                "eucc-config": {"format": "%s", "vct": "eucc",
                  "credential_metadata": {"display": [{"name": "EUCC"}], "claims": [{"path": %s}]}}
              }
            }
            """.formatted(format, path);
        var client = RestClient.builder();
        var server = MockRestServiceServer.bindTo(client).build();
        server.expect(requestTo("https://issuer.example/.well-known/openid-credential-issuer"))
                .andRespond(withSuccess(metadata, MediaType.APPLICATION_JSON));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var retriever = new CredentialIssuerMetadataRetriever(factory.getValidator(), null, client.build(), null, null);
            var issuer = retriever.fetchCredentialIssuerFromMetadataRequest(URI.create("https://issuer.example"));
            assertNotNull(issuer);
            assertTrue(issuer.getCredentialConfiguration().containsKey("EBWOID"));
            assertEquals(expectedCount, issuer.getCredentialConfiguration().size());
        }
        server.verify();
    }
}

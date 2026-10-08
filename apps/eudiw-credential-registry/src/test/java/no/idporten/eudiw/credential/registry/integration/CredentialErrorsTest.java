package no.idporten.eudiw.credential.registry.integration;

import jakarta.validation.Validation;
import no.idporten.eudiw.credential.registry.api.CredentialErrorsController;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CredentialErrorsTest {
    private static final URI ISSUER = URI.create("https://agent.paradym.id/oid4vci/a3614c5c-aad7-4c72-b1be-2837ffd644af");
    private static final String METADATA_URL = "https://agent.paradym.id/.well-known/openid-credential-issuer/oid4vci/a3614c5c-aad7-4c72-b1be-2837ffd644af";

    @Test
    void errorsUseFreshMetadataAndIncludeOnlyInvalidCredentials() throws Exception {
        String metadata;
        try (var stream = getClass().getResourceAsStream("/brreg-issuer-metadata.json")) {
            metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var mapper = JsonMapper.builder().build();
        var invalid = mapper.readTree(metadata);
        ((ObjectNode) invalid.get("credential_configurations_supported").get("cmtty9uoi000302s64nstreth")
                .get("credential_metadata").get("claims").get(0)).set("path", mapper.readTree("[\"organisation\",-1]"));
        var unparseable = mapper.readTree(metadata);
        ((ObjectNode) unparseable.get("credential_configurations_supported").get("cmtty9uoi000302s64nstreth")
                .get("credential_metadata").get("claims").get(0)).put("path", "not an array");
        var rp = RestClient.builder().baseUrl("https://rp.example/credential-issuers");
        var rpServer = MockRestServiceServer.bindTo(rp).build();
        var external = RestClient.builder();
        var externalServer = MockRestServiceServer.bindTo(external).build();
        var responses = java.util.List.of(metadata, mapper.writeValueAsString(invalid), mapper.writeValueAsString(unparseable));
        for (String response : responses) {
            rpServer.expect(requestTo("https://rp.example/credential-issuers"))
                    .andRespond(withSuccess("{\"credential_issuer_urls\":[\"" + ISSUER + "\"]}", MediaType.APPLICATION_JSON));
            externalServer.expect(requestTo(METADATA_URL))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        }
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var retriever = new CredentialIssuerMetadataRetriever(factory.getValidator(), rp.build(), external.build(), null, null);
            assertTrue(retriever.getCredentialErrors(ISSUER).isEmpty());
            var errors = retriever.getCredentialErrors(ISSUER);
            assertEquals(1, errors.size());
            assertEquals("EUCC", errors.getFirst().name());
            assertTrue(errors.getFirst().errors().getFirst().contains("credential_metadata.claims[0].path"));
            var parsingErrors = retriever.getCredentialErrors(ISSUER);
            assertEquals(1, parsingErrors.size());
            assertEquals("EUCC", parsingErrors.getFirst().name());
            assertTrue(parsingErrors.getFirst().errors().getFirst().contains("credential_metadata.claims[0].path"));
            assertNull(retriever.getListOfIssuer(), "Reading errors must not change the crawler cache");
        }
        rpServer.verify();
        externalServer.verify();
    }

    @Test
    void unregisteredIssuerCannotBeFetchedThroughErrorEndpoint() {
        var rp = RestClient.builder().baseUrl("https://rp.example/credential-issuers");
        var rpServer = MockRestServiceServer.bindTo(rp).build();
        rpServer.expect(requestTo("https://rp.example/credential-issuers"))
                .andRespond(withSuccess("{\"credential_issuer_urls\":[]}", MediaType.APPLICATION_JSON));
        RestClient external = mock(RestClient.class);
        var retriever = new CredentialIssuerMetadataRetriever(null, rp.build(), external, null, null);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> retriever.getCredentialErrors(ISSUER)).getStatusCode());
        verifyNoInteractions(external);
        rpServer.verify();
    }

    @Test
    void errorEndpointRequiresInternalApiKey() {
        var retriever = mock(CredentialIssuerMetadataRetriever.class);
        var config = new ConfigProperties(Duration.ofSeconds(3), Duration.ofSeconds(3), URI.create("https://rp.example"), "X-API-KEY", "test-key");
        var controller = new CredentialErrorsController(retriever, config);
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
                () -> controller.errors(new CredentialErrorsController.Request(ISSUER), new MockHttpServletRequest())).getStatusCode());
        verifyNoInteractions(retriever);
    }
}

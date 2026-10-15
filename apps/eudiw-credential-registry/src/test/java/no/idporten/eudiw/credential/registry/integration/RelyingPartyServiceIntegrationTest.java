package no.idporten.eudiw.credential.registry.integration;

import io.micrometer.core.instrument.Counter;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Relying party service integration tests")
class RelyingPartyServiceIntegrationTest {

    private static final String RP_SERVICE_URL = "https://rp-service.example.com";

    private MockRestServiceServer server;
    private Counter counter;
    private RelyingPartyServiceIntegration integration;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(RP_SERVICE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        counter = mock(Counter.class);
        integration = new RelyingPartyServiceIntegration(builder.build(), counter);
    }

    @Test
    @DisplayName("retrieveCredentialIssuerUrlsFromRPService filters out invalid URLs")
    void retrieveCredentialIssuerUrlsFromRPService_filtersInvalidUrls() {
        server.expect(requestTo(RP_SERVICE_URL))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "credential_issuer_urls": [
                            "https://issuer.example.com",
                            "http://issuer.example.com",
                            "https:///path",
                            null
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<URI> result = integration.retrieveCredentialIssuerUrlsFromRPService();

        assertEquals(List.of(URI.create("https://issuer.example.com")), result);
        verify(counter, never()).increment();
        server.verify();
    }

    @Test
    @DisplayName("retrieveCredentialIssuerUrlsFromRPService increments counter and throws on resource access failure")
    void retrieveCredentialIssuerUrlsFromRPService_whenResourceAccessFails() {
        server.expect(requestTo(RP_SERVICE_URL))
                .andExpect(method(GET))
                .andRespond(withException(new IOException("boom")));

        CredentialRegisterException exception = assertThrows(
                CredentialRegisterException.class,
                () -> integration.retrieveCredentialIssuerUrlsFromRPService()
        );

        assertInstanceOf(ResourceAccessException.class, exception.getCause());
        assertEquals("Failed to connect to rp-service", exception.getErrorDescription());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        verify(counter).increment();
        server.verify();
    }

    @Test
    @DisplayName("retrieveCredentialIssuerUrlsFromRPService throws on unexpected errors")
    void retrieveCredentialIssuerUrlsFromRPService_whenUnexpectedErrorOccurs() {
        server.expect(requestTo(RP_SERVICE_URL))
                .andExpect(method(GET))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        CredentialRegisterException exception = assertThrows(
                CredentialRegisterException.class,
                () -> integration.retrieveCredentialIssuerUrlsFromRPService()
        );

        assertEquals("Failed to fetch issuer-server uris from rp-service", exception.getErrorDescription());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        verify(counter, never()).increment();
        server.verify();
    }

    @Test
    @DisplayName("isHttps accepts HTTPS URIs and rejects non-HTTPS URIs")
    void isHttps_acceptsOnlyHttps() {
        assertTrue(integration.isHttps(URI.create("https://issuer.example.com")));
        assertFalse(integration.isHttps(URI.create("http://issuer.example.com")));
        assertFalse(integration.isHttps(URI.create("http://localhost:8080/")));
        assertFalse(integration.isHttps(URI.create("issuer.example.com")));
    }

    @Test
    @DisplayName("emptyHost rejects URIs that have no host")
    void emptyHost_rejectsMissingHost() {
        assertFalse(integration.emptyHost(URI.create("https://issuer.example.com")));
        assertTrue(integration.emptyHost(URI.create("https:///path")));
    }
}

package no.idporten.eudiw.credential.registry.integration;

import io.micrometer.core.instrument.Counter;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.idporten.eudiw.credential.registry.configuration.ConfigProperties;
import no.idporten.eudiw.credential.registry.configuration.CredentialRegisterConfiguration;
import no.idporten.eudiw.credential.registry.exception.CredentialRegisterException;
import no.idporten.eudiw.credential.registry.response.CredentialRegisterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CredentialRegistryV2RefreshTest {

    private static final URI RP_SERVICE_URL = URI.create("https://rp-service.example/v2/credential-issuers");
    private static final String ISSUER_URL = "https://issuer.example";
    private static final String API_KEY = "test-api-key";

    private MockRestServiceServer registerServer;
    private MockRestServiceServer metadataServer;
    private ValidatorFactory validatorFactory;
    private CredentialRegisterService credentialRegisterService;

    @BeforeEach
    void setUp() {
        var properties = new ConfigProperties(
            Duration.ofSeconds(5), Duration.ofSeconds(3), RP_SERVICE_URL, "X-API-KEY", API_KEY);
        var configuration = new CredentialRegisterConfiguration(properties);
        var registerClient = configuration.restClientRpService(RestClient.builder()).mutate();
        registerServer = MockRestServiceServer.bindTo(registerClient).build();
        var metadataClient = configuration.restClientExternalApi(RestClient.builder()).mutate();
        metadataServer = MockRestServiceServer.bindTo(metadataClient).build();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        var integration = new RelyingPartyServiceIntegration(registerClient.build(), mock(Counter.class));
        var retriever = new CredentialIssuerMetadataRetriever(
            validatorFactory.getValidator(), metadataClient.build(), mock(Counter.class), integration,
            new ObjectMapper());
        credentialRegisterService = new CredentialRegisterService(retriever);
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
        registerServer.verify();
        metadataServer.verify();
    }

    @Test
    @DisplayName("Should refresh credentials through V2 and retain the cache on access failure without V1 fallback")
    void shouldRefreshCredentialsThroughV2AndRetainCacheOnAccessFailureWithoutV1Fallback() {
        expectIssuerMetadata();

        credentialRegisterService.updateCredentialMetadataRetriever();
        var cachedCredentials = credentialRegisterService.getCredentials();
        assertEquals(1, cachedCredentials.credentials().size());
        assertEquals(ISSUER_URL, cachedCredentials.credentials().getFirst().credentialIssuer());
        assertEquals("dc+sd-jwt", cachedCredentials.credentials().getFirst().format());

        registerServer.reset();
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andExpect(method(GET))
            .andExpect(header("X-API-KEY", API_KEY))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        var exception = assertThrows(CredentialRegisterException.class,
            credentialRegisterService::updateCredentialMetadataRetriever);

        var cause = assertInstanceOf(RestClientResponseException.class, exception.getCause());
        assertEquals(HttpStatus.UNAUTHORIZED, cause.getStatusCode());
        assertSame(cachedCredentials, credentialRegisterService.getCredentials());
    }

    @Test
    @DisplayName("Should clear cached credentials when V2 returns an empty issuer list")
    void shouldClearCachedCredentialsWhenV2ReturnsAnEmptyIssuerList() {
        expectIssuerMetadata();
        credentialRegisterService.updateCredentialMetadataRetriever();
        assertFalse(credentialRegisterService.getCredentials().credentials().isEmpty());

        registerServer.reset();
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andExpect(header("X-API-KEY", API_KEY))
            .andRespond(withSuccess("{\"credential_issuer_urls\": []}", MediaType.APPLICATION_JSON));

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertTrue(credentialRegisterService.getCredentials().credentials().isEmpty());
    }

    private void expectIssuerMetadata() {
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andExpect(method(GET))
            .andExpect(header("X-API-KEY", API_KEY))
            .andRespond(withSuccess("{\"credential_issuer_urls\": [\"" + ISSUER_URL + "\"]}",
                MediaType.APPLICATION_JSON));
        metadataServer.expect(requestTo(ISSUER_URL + "/.well-known/openid-credential-issuer"))
            .andExpect(method(GET))
            .andExpect(request -> assertNull(request.getHeaders().getFirst("X-API-KEY")))
            .andRespond(withSuccess("""
                {
                  "credential_issuer": "https://issuer.example",
                  "credential_configurations_supported": {
                    "example": {"format": "dc+sd-jwt", "vct": "urn:example:credential"}
                  }
                }
                """, MediaType.APPLICATION_JSON));
    }
}

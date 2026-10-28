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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @DisplayName("Should keep previously available credentials when access to the issuer register is denied")
    void shouldKeepPreviouslyAvailableCredentialsWhenAccessToTheIssuerRegisterIsDenied() {
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
    @DisplayName("Should remove previously available credentials when the issuer register returns no issuers")
    void shouldRemovePreviouslyAvailableCredentialsWhenTheIssuerRegisterReturnsNoIssuers() {
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

    @Test
    @DisplayName("Should update available credentials even when another issuer has an invalid address")
    void shouldUpdateAvailableCredentialsEvenWhenAnotherIssuerHasAnInvalidAddress() {
        expectIssuerMetadata("\"https://bad host\", \"https://issuer.example\"");

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertEquals(1, credentialRegisterService.getCredentials().credentials().size());
        assertEquals(ISSUER_URL, credentialRegisterService.getCredentials().credentials().getFirst().credentialIssuer());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[\"http://issuer.example\", \"https:///issuer\", null]", "[\"https://bad host\"]", "[null]"})
    @DisplayName("Should keep previously available credentials when none of the registered issuer addresses can be used")
    void shouldKeepPreviouslyAvailableCredentialsWhenNoneOfTheRegisteredIssuerAddressesCanBeUsed(String issuerUrls) {
        expectIssuerMetadata();
        credentialRegisterService.updateCredentialMetadataRetriever();
        var previousCredentials = credentialRegisterService.getCredentials();
        registerServer.reset();
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andRespond(withSuccess("{\"credential_issuer_urls\": " + issuerUrls + "}", MediaType.APPLICATION_JSON));

        var exception = assertThrows(CredentialRegisterException.class,
            credentialRegisterService::updateCredentialMetadataRetriever);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        assertSame(previousCredentials, credentialRegisterService.getCredentials());
    }

    private void expectIssuerMetadata() {
        expectIssuerMetadata("\"" + ISSUER_URL + "\"");
    }

    private void expectIssuerMetadata(String issuerUrls) {
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andExpect(method(GET))
            .andExpect(header("X-API-KEY", API_KEY))
            .andRespond(withSuccess("{\"credential_issuer_urls\": [" + issuerUrls + "]}",
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

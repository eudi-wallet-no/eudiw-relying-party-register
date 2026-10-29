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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
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

    @ParameterizedTest
    @MethodSource("failedMetadataResponses")
    @DisplayName("Should allow startup without available credentials and publish them when issuer metadata becomes available")
    void shouldAllowStartupWithoutAvailableCredentialsAndPublishThemWhenIssuerMetadataBecomesAvailable(
        ResponseCreator response) {
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andRespond(withSuccess("{\"credential_issuer_urls\": [\"https://issuer.example\"]}", MediaType.APPLICATION_JSON));
        metadataServer.expect(requestTo(ISSUER_URL + "/.well-known/openid-credential-issuer"))
            .andRespond(response);

        assertDoesNotThrow(credentialRegisterService::init);
        assertNull(credentialRegisterService.getCredentials());
        registerServer.verify();
        metadataServer.verify();
        registerServer.reset();
        metadataServer.reset();
        expectIssuerMetadata();

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertEquals(1, credentialRegisterService.getCredentials().credentials().size());
        assertEquals(ISSUER_URL, credentialRegisterService.getCredentials().credentials().getFirst().credentialIssuer());
    }

    @Test
    @DisplayName("Should allow startup without available credentials and publish them when the issuer register becomes available")
    void shouldAllowStartupWithoutAvailableCredentialsAndPublishThemWhenTheIssuerRegisterBecomesAvailable() {
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertDoesNotThrow(credentialRegisterService::init);
        assertNull(credentialRegisterService.getCredentials());
        registerServer.verify();
        registerServer.reset();
        expectIssuerMetadata();

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertEquals(1, credentialRegisterService.getCredentials().credentials().size());
        assertEquals(ISSUER_URL, credentialRegisterService.getCredentials().credentials().getFirst().credentialIssuer());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 500})
    @DisplayName("Should keep previously available credentials when the issuer register reports an error")
    void shouldKeepPreviouslyAvailableCredentialsWhenTheIssuerRegisterReportsAnError(int status) {
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
            .andRespond(withStatus(HttpStatus.valueOf(status)));

        var exception = assertThrows(CredentialRegisterException.class,
            credentialRegisterService::updateCredentialMetadataRetriever);

        var cause = assertInstanceOf(RestClientResponseException.class, exception.getCause());
        assertEquals(HttpStatus.valueOf(status), cause.getStatusCode());
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

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{\"credential_issuer_urls\": null}"})
    @DisplayName("Should keep previously available credentials when the issuer register returns an incomplete response")
    void shouldKeepPreviouslyAvailableCredentialsWhenTheIssuerRegisterReturnsAnIncompleteResponse(String response) {
        expectIssuerMetadata();
        credentialRegisterService.updateCredentialMetadataRetriever();
        var previousCredentials = credentialRegisterService.getCredentials();
        registerServer.reset();
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        var exception = assertThrows(CredentialRegisterException.class,
            credentialRegisterService::updateCredentialMetadataRetriever);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        assertSame(previousCredentials, credentialRegisterService.getCredentials());
    }

    @ParameterizedTest
    @MethodSource("failedMetadataResponses")
    @DisplayName("Should keep previously available credentials when all issuers fail to provide valid metadata and update them when available again")
    void shouldKeepPreviouslyAvailableCredentialsWhenAllIssuersFailToProvideValidMetadataAndUpdateThemWhenAvailableAgain(
        ResponseCreator response) {
        expectIssuerMetadata();
        credentialRegisterService.updateCredentialMetadataRetriever();
        var previousCredentials = credentialRegisterService.getCredentials();
        registerServer.reset();
        metadataServer.reset();
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andRespond(withSuccess("{\"credential_issuer_urls\": [\"https://issuer.example\", \"https://unavailable.example\"]}",
                MediaType.APPLICATION_JSON));
        metadataServer.expect(requestTo(ISSUER_URL + "/.well-known/openid-credential-issuer"))
            .andRespond(response);
        metadataServer.expect(requestTo("https://unavailable.example/.well-known/openid-credential-issuer"))
            .andRespond(response);

        var exception = assertThrows(CredentialRegisterException.class,
            credentialRegisterService::updateCredentialMetadataRetriever);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        assertSame(previousCredentials, credentialRegisterService.getCredentials());
        registerServer.verify();
        metadataServer.verify();
        registerServer.reset();
        metadataServer.reset();
        expectIssuerMetadata();

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertNotSame(previousCredentials, credentialRegisterService.getCredentials());
        assertEquals(1, credentialRegisterService.getCredentials().credentials().size());
        assertEquals(ISSUER_URL, credentialRegisterService.getCredentials().credentials().getFirst().credentialIssuer());
    }

    @Test
    @DisplayName("Should publish available credentials even when another issuer cannot provide metadata")
    void shouldPublishAvailableCredentialsEvenWhenAnotherIssuerCannotProvideMetadata() {
        expectIssuerMetadata("\"https://issuer.example\", \"https://unavailable.example\"");
        metadataServer.expect(requestTo("https://unavailable.example/.well-known/openid-credential-issuer"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertEquals(1, credentialRegisterService.getCredentials().credentials().size());
        assertEquals(ISSUER_URL, credentialRegisterService.getCredentials().credentials().getFirst().credentialIssuer());
    }

    @Test
    @DisplayName("Should remove previously available credentials when valid issuer metadata no longer offers credentials")
    void shouldRemovePreviouslyAvailableCredentialsWhenValidIssuerMetadataNoLongerOffersCredentials() {
        expectIssuerMetadata();
        credentialRegisterService.updateCredentialMetadataRetriever();
        assertFalse(credentialRegisterService.getCredentials().credentials().isEmpty());
        registerServer.reset();
        metadataServer.reset();
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andRespond(withSuccess("{\"credential_issuer_urls\": [\"https://issuer.example\"]}", MediaType.APPLICATION_JSON));
        metadataServer.expect(requestTo(ISSUER_URL + "/.well-known/openid-credential-issuer"))
            .andRespond(withSuccess("{\"credential_issuer\": \"https://issuer.example\", \"credential_configurations_supported\": {}}",
                MediaType.APPLICATION_JSON));

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertTrue(credentialRegisterService.getCredentials().credentials().isEmpty());
    }

    @Test
    @DisplayName("Should publish valid credentials when the same issuer also offers invalid configurations")
    void shouldPublishValidCredentialsWhenTheSameIssuerAlsoOffersInvalidConfigurations() {
        registerServer.expect(requestTo(RP_SERVICE_URL))
            .andRespond(withSuccess("{\"credential_issuer_urls\": [\"https://issuer.example\"]}", MediaType.APPLICATION_JSON));
        metadataServer.expect(requestTo(ISSUER_URL + "/.well-known/openid-credential-issuer"))
            .andRespond(withSuccess("""
                {
                  "credential_issuer": "https://issuer.example",
                  "credential_configurations_supported": {
                    "invalid": {},
                    "example": {"format": "dc+sd-jwt", "vct": "urn:example:credential"}
                  }
                }
                """, MediaType.APPLICATION_JSON));

        credentialRegisterService.updateCredentialMetadataRetriever();

        assertEquals(1, credentialRegisterService.getCredentials().credentials().size());
        assertEquals("example", credentialRegisterService.getCredentials().credentials().getFirst().credentialConfigurationId());
    }

    private static Stream<ResponseCreator> failedMetadataResponses() {
        return Stream.of(
            withException(new UnknownHostException("issuer.example")),
            withException(new SocketTimeoutException("Metadata request timed out")),
            withStatus(HttpStatus.SERVICE_UNAVAILABLE),
            withSuccess("", MediaType.APPLICATION_JSON),
            withSuccess("null", MediaType.APPLICATION_JSON),
            withSuccess("{", MediaType.APPLICATION_JSON),
            withSuccess("{}", MediaType.APPLICATION_JSON),
            withSuccess("{\"credential_issuer\": \"https://issuer.example\", \"credential_configurations_supported\": {\"example\": {}}}",
                MediaType.APPLICATION_JSON),
            withSuccess("{\"credential_issuer\": \"https://issuer.example\", \"credential_configurations_supported\": {\"example\": null}}",
                MediaType.APPLICATION_JSON),
            withSuccess("{\"credential_issuer\": \"https://issuer.example\", \"credential_configurations_supported\": {\"example\": 123}}",
                MediaType.APPLICATION_JSON));
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

package no.idporten.eudiw.rp.register.lookup.service.credentialsservice;

import no.idporten.eudiw.rp.register.lookup.service.MockWebServerConfiguration;
import no.idporten.eudiw.rp.register.lookup.service.exception.*;
import no.idporten.eudiw.rp.register.lookup.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.register.lookup.service.exception.BadRequestException;
import no.idporten.eudiw.rp.register.lookup.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialsResource;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local-test")
@Import(MockWebServerConfiguration.class)
@DisplayName("When using the credentials service")
public class CredentialsServiceTests {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private MockWebServer mockWebServer;

    @Test
    @DisplayName("then deserialization and validation succeeds when credentials are valid")
    public void testRestClientDeserializationOfValidCredentials() throws Exception {
        CredentialsResource credentials = ResourceGenerator.generateCredentialsResource();
        String credentialsResponseBody = objectMapper.writeValueAsString(credentials);

        MockResponse mockValidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(credentialsResponseBody);
        mockWebServer.enqueue(mockValidResponse);

        CredentialsResource actualResponse = credentialsService.getAvailableCredentials();

        assertEquals(credentials, actualResponse);
    }

    @Test
    @DisplayName("then a single credential can be retrieved by issuer and configuration ID")
    public void testGetExistingCredential() throws Exception {
        CredentialsResource credentials = ResourceGenerator.generateCredentialsResource();
        String credentialsResponseBody = objectMapper.writeValueAsString(credentials);

        MockResponse mockValidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(credentialsResponseBody);
        mockWebServer.enqueue(mockValidResponse);

        // choose a credential to fetch by issuer and configuration ID
        CredentialResource expectedResponse = credentials.credentials().getFirst();

        CredentialResource actualResponse =
            credentialsService.getCredential(
                expectedResponse.getIssuer(), expectedResponse.getConfigurationId());

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("then service throws NotFoundException when credential does not exist")
    public void testGetNonexistentCredential() throws Exception {
        CredentialsResource credentials = ResourceGenerator.generateCredentialsResource();
        String credentialsResponseBody = objectMapper.writeValueAsString(credentials);
        MockResponse mockValidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(credentialsResponseBody);
        mockWebServer.enqueue(mockValidResponse);

        String randomIssuer = TestDataGenerator.generateName();
        String randomConfigurationId = TestDataGenerator.generateName();
        assertThrowsExactly(
            NotFoundException.class,
            () -> credentialsService.getCredential(randomIssuer, randomConfigurationId));
    }

    @Test
    @DisplayName("then validation ensures invalid credentials are omitted from the result")
    public void testInvalidCredentialsOmittedFromCredentialsResource() throws Exception {
        CredentialResource validCredential =
            ResourceGenerator.generateCredentialResource();

        String invalidFormatType = "mdoc";
        CredentialResource invalidCredential = new CredentialResource(
            invalidFormatType,
            validCredential.getIssuer(),
            validCredential.getIssuerDisplays(),
            validCredential.getConfigurationId(),
            validCredential.getCredentialType(),
            validCredential.getMetadata());

        CredentialsResource credentials = new CredentialsResource(List.of(invalidCredential, validCredential));

        String credentialsResponseBody = objectMapper.writeValueAsString(credentials);

        MockResponse mockInvalidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(credentialsResponseBody);
        mockWebServer.enqueue(mockInvalidResponse);

        CredentialsResource credentialsResource = credentialsService.getAvailableCredentials();
        assertEquals(1, credentialsResource.credentials().size());
        assertTrue(credentialsResource.credentials().contains(validCredential));
        assertFalse(credentialsResource.credentials().contains(invalidCredential));
    }

    @Nested
    @DisplayName("when the response error handler fires")
    class ResponseErrorHandlerTests {

        @Test
        @DisplayName("then the ResponseErrorHandler properly handles valid 4xx responses")
        void testErrorResponseExceptionThrownOnValid4xxResponse() {
            MockResponse badRequestMockResponse = new MockResponse().setResponseCode(400);
            mockWebServer.enqueue(badRequestMockResponse);

            assertThrowsExactly(BadRequestException.class,
                                () -> credentialsService.getAvailableCredentials()
            );
        }

        @Test
        @DisplayName("then the ResponseErrorHandler properly handles valid 5xx responses")
        void testErrorResponseExceptionThrownOnValid5xxResponse() {
            MockResponse serverErrorMockResponse = new MockResponse().setResponseCode(500);
            mockWebServer.enqueue(serverErrorMockResponse);

            assertThrowsExactly(ErrorResponseException.class,
                                () -> credentialsService.getAvailableCredentials()
            );
        }

        @Test
        @DisplayName("then the ResponseErrorHandler properly handles unrecognized responses")
        void testErrorResponseExceptionThrownOnUnrecognizedResponses() {
            MockResponse unrecognizedMockResponse = new MockResponse().setResponseCode(300);
            mockWebServer.enqueue(unrecognizedMockResponse);

            assertThrowsExactly(UnrecognizedErrorResponseException.class,
                                () -> credentialsService.getAvailableCredentials()
            );
        }
    }
}

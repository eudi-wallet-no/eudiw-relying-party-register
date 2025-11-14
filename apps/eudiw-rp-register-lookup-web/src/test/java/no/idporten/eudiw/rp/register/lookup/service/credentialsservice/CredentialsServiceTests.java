package no.idporten.eudiw.rp.register.lookup.service.credentialsservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import no.idporten.eudiw.rp.register.lookup.service.MockWebServerConfiguration;
import no.idporten.eudiw.rp.register.lookup.service.exception.BadRequestException;
import no.idporten.eudiw.rp.register.lookup.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.service.exception.ResponseValidationException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialMetadata;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialsResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.Display;
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

import java.util.List;
import java.util.Set;

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

    @DisplayName("then deserialization and validation succeeds when credentials are valid")
    @Test
    public void testRestClientDeserializationOfValidCredentials() throws Exception {
        Display display = new Display(TestDataGenerator.generateName(), "no");
        var displays = List.of(display);
        var paths = List.of("family_name");
        var claims = List.of(new CredentialMetadata.Claims(paths, displays));
        List<CredentialMetadata> metadata = List.of(
            new CredentialMetadata(displays, claims));
        CredentialsResource credentials = new CredentialsResource(List.of(
            new CredentialResource("mso_mdoc", "issuer", display, "config-id", "type", metadata)
        ));

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

    @DisplayName("then validation fails when credentials are invalid")
    @Test
    public void foobar() throws Exception {
        String invalidFormatType = "mdoc";

        Display display = new Display(TestDataGenerator.generateName(), "no");
        var displays = List.of(display);
        var paths = List.of("family_name");
        var claims = List.of(new CredentialMetadata.Claims(paths, displays));
        List<CredentialMetadata> metadata = List.of(
            new CredentialMetadata(displays, claims));
        CredentialsResource credentials = new CredentialsResource(List.of(
            new CredentialResource(invalidFormatType, "issuer", display, "config-id", "type", metadata)
        ));

        String credentialsResponseBody = objectMapper.writeValueAsString(credentials);

        MockResponse mockInvalidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(credentialsResponseBody);
        mockWebServer.enqueue(mockInvalidResponse);

        Set<ConstraintViolation<?>> validationViolations =
            assertThrowsExactly(
                ResponseValidationException.class,
                () -> credentialsService.getAvailableCredentials()
            ).getViolations();
        assertFalse(validationViolations.isEmpty());
    }

    @Nested
    @DisplayName("when the response error handler fires")
    class ResponseErrorHandlerTests {

        @DisplayName("then the ResponseErrorHandler properly handles valid 4xx responses")
        @Test
        void testErrorResponseExceptionThrownOnValid4xxResponse() {
            MockResponse badRequestMockResponse = new MockResponse().setResponseCode(400);
            mockWebServer.enqueue(badRequestMockResponse);

            assertThrowsExactly(BadRequestException.class,
                                () -> credentialsService.getAvailableCredentials()
            );
        }

        @DisplayName("then the ResponseErrorHandler properly handles valid 5xx responses")
        @Test
        void testErrorResponseExceptionThrownOnValid5xxResponse() {
            MockResponse serverErrorMockResponse = new MockResponse().setResponseCode(500);
            mockWebServer.enqueue(serverErrorMockResponse);

            assertThrowsExactly(ErrorResponseException.class,
                                () -> credentialsService.getAvailableCredentials()
            );
        }

        @DisplayName("then the ResponseErrorHandler properly handles unrecognized responses")
        @Test
        void testErrorResponseExceptionThrownOnUnrecognizedResponses() {
            MockResponse unrecognizedMockResponse = new MockResponse().setResponseCode(300);
            mockWebServer.enqueue(unrecognizedMockResponse);

            assertThrowsExactly(UnrecognizedErrorResponseException.class,
                                () -> credentialsService.getAvailableCredentials()
            );
        }
    }
}

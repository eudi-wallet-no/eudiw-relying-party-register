package no.eudiw.rp.register.integrations.certificateservice;

import no.eudiw.rp.register.exception.ErrorResponseException;
import no.eudiw.rp.register.exception.UnauthorizedRequestException;
import no.eudiw.rp.register.integrations.certificateservice.config.RelyingPartyCertificateServiceConfig;
import no.eudiw.rp.register.integrations.certificateservice.config.RelyingPartyCertificateServiceProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.exc.StreamReadException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CertificateServiceClientTest {

    private MockWebServer server;
    private CertificateServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        var properties = new RelyingPartyCertificateServiceProperties(
            new RelyingPartyCertificateServiceProperties.CaServiceApi(
                server.url("/").toString().replaceAll("/$", ""), "X-API-KEY", "test-key"),
            new RelyingPartyCertificateServiceProperties.RestClient(1000, 1000),
            "access-ca"
        );
        client = new CertificateServiceClient(new RelyingPartyCertificateServiceConfig(properties).caRestClient());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void sendsRevocationToSelectedCaWithConfiguredApiKey() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(204));

        HttpStatusCode status = client.revokeCertificate("12345", 0, "issuer-ca");

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(HttpStatus.NO_CONTENT, status);
        assertEquals("PUT", request.getMethod());
        assertEquals("/v1/certs/issuer-ca", request.getPath());
        assertEquals("test-key", request.getHeader("X-API-KEY"));
        JsonNode body = new ObjectMapper().readTree(request.getBody().readUtf8());
        assertEquals("12345", body.get("serial_number").asText());
        assertEquals(0, body.get("reason").asInt());
    }

    @ParameterizedTest
    @MethodSource("errorResponses")
    void preservesExternalErrorTypes(int status, String body, Class<? extends Exception> errorType) {
        server.enqueue(new MockResponse()
            .setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body));

        assertThrows(errorType, () -> client.revokeCertificate("12345", 0, "issuer-ca"));
    }

    private static Stream<Arguments> errorResponses() {
        return Stream.of(
            arguments(401, "", UnauthorizedRequestException.class),
            arguments(400, "{\"error\":\"invalid_request\"}", ErrorResponseException.class),
            arguments(500, "not-json", StreamReadException.class)
        );
    }
}

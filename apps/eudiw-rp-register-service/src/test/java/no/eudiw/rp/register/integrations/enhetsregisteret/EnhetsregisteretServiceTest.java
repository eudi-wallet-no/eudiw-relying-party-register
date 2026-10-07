package no.eudiw.rp.register.integrations.enhetsregisteret;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.integrations.enhetsregisteret.config.EnhetsregisteretServiceConfig;
import no.eudiw.rp.register.integrations.enhetsregisteret.config.EnhetsregisteretServiceProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EnhetsregisteretServiceTest {

    private MockWebServer server;
    private ValidatorFactory validatorFactory;
    private EnhetsregisteretService client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        var properties = new EnhetsregisteretServiceProperties(
            List.of("6100"),
            server.url("/enheter").uri(),
            new EnhetsregisteretServiceProperties.RestClient(1000, 1000)
        );
        client = new EnhetsregisteretServiceConfig(properties)
            .enhetsregisteretService(validatorFactory.getValidator());
    }

    @AfterEach
    void tearDown() throws IOException {
        validatorFactory.close();
        server.close();
    }

    @Test
    void mapsOrganizationNameAndPublicSectorFromExternalResponse() throws InterruptedException {
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"navn\":\"Organization\",\"institusjonellSektorkode\":{\"kode\":\"2100, 6100\"}}"));

        var response = client.queryOrgno("987654321");

        assertEquals("Organization", response.name());
        assertTrue(response.publicSector());
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/enheter/987654321", request.getPath());
    }

    @Test
    void preservesNotFoundForMissingOrganization() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThrows(NotFoundException.class, () -> client.queryOrgno("987654321"));
    }

    @Test
    void rejectsInvalidExternalSectorCodes() {
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"navn\":\"Organization\",\"institusjonellSektorkode\":{\"kode\":\"invalid\"}}"));

        assertThrows(NotFoundException.class, () -> client.queryOrgno("987654321"));
    }
}

package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.integrations.registerservice.config.RelyingPartiesServiceConfig;
import no.idporten.eudiw.rp.admin.integrations.registerservice.config.RelyingPartiesServiceProperties;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class V2RegisterClientTests {

    private MockRestServiceServer server;
    private V2RegisterClient client;

    @BeforeEach
    void setUp() {
        var properties = new RelyingPartiesServiceProperties(
            new RelyingPartiesServiceProperties.RegisterServiceApi("https://register.test", "X-API-KEY", "test-api-key"),
            new RelyingPartiesServiceProperties.RestClient(1000, 1000), null);
        RestClient.Builder builder = new RelyingPartiesServiceConfig(properties).v2RestClient().mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new V2RegisterClient(builder.build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    @DisplayName("Retrieve all registrations for the selected organization")
    void shouldRetrieveAllRegistrationsForOrganization() {
        server.expect(requestTo("https://register.test/v2/registrations?orgno=123456785"))
            .andExpect(header("X-API-KEY", "test-api-key"))
            .andRespond(withSuccess("""
                [{"id":"00000000-0000-0000-0000-000000000001","serviceTradeName":"Kontrollteneste",
                  "relyingPartyEntitlements":[{"displayName":"Utferdar"}],
                  "createdMs":1700000000000,"lastUpdatedMs":1710000000000}]
                """, MediaType.APPLICATION_JSON));

        assertEquals(List.of(new V2RegisterClient.Registration(new UUID(0, 1), "Kontrollteneste",
            List.of(new V2RegisterClient.Role("Utferdar")), 1700000000000L, 1710000000000L)),
            client.getRegistrations("123456785"));
    }

    @Test
    @DisplayName("Report a missing response rather than show that the organization has no registrations")
    void shouldDistinguishMissingResponseFromNoRegistrations() {
        server.expect(requestTo("https://register.test/v2/registrations?orgno=123456785"))
            .andRespond(withSuccess());
        server.expect(requestTo("https://register.test/v2/registrations?orgno=123456785"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var exception = assertThrows(UnrecognizedErrorResponseException.class, () -> client.getRegistrations("123456785"));
        assertEquals("Unable to retrieve registrations from register service: response body is empty", exception.getMessage());
        assertEquals(List.of(), client.getRegistrations("123456785"));
    }

    @Test
    @DisplayName("Report when register service cannot be reached to retrieve registrations")
    void shouldReportWhenRegisterServiceCannotBeReached() {
        IOException connectionFailure = new IOException("Connection refused");
        server.expect(requestTo("https://register.test/v2/registrations?orgno=123456785"))
            .andRespond(withException(connectionFailure));

        var exception = assertThrows(AdminServiceException.class, () -> client.getRegistrations("123456785"));

        assertEquals("Unable to contact register service to retrieve registrations", exception.getMessage());
        assertSame(connectionFailure, exception.getCause().getCause());
    }

    @Test
    @DisplayName("Report register service failure rather than show that the organization has no registrations")
    void shouldReportFailureToRetrieveRegistrations() {
        server.expect(requestTo("https://register.test/v2/registrations?orgno=123456785"))
            .andRespond(withServerError());

        assertThrows(UnrecognizedErrorResponseException.class, () -> client.getRegistrations("123456785"));
    }
}

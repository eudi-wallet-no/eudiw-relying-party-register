package no.idporten.eudiw.rp.admin.integrations.registerservice;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.integrations.registerservice.config.RelyingPartiesServiceConfig;
import no.idporten.eudiw.rp.admin.integrations.registerservice.config.RelyingPartiesServiceProperties;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.RelyingPartyNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEntitlementResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class V2RegisterClientTests {

    private MockRestServiceServer server;
    private V2RegisterClient client;

    @BeforeEach
    void setUp() {
        var properties = new RelyingPartiesServiceProperties(
            new RelyingPartiesServiceProperties.RegisterServiceApi("https://register.test", "X-API-KEY", "test-api-key"),
            new RelyingPartiesServiceProperties.RestClient(1000, 1000), null);
        RestClient.Builder builder = new RelyingPartiesServiceConfig(properties).v2RestClient(RestClient.builder()).mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new V2RegisterClient(builder.build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    private static final UUID RELYING_PARTY_INSTANCE_ID = new UUID(0, 1);
    private static final String REGISTRATION_URL = "https://register.test/v2/registrations/" + RELYING_PARTY_INSTANCE_ID;

    @Test
    @DisplayName("Should preserve literal search text and all role filters when requesting a sorted search page")
    void shouldPreserveLiteralSearchTextAndAllRoleFiltersOnSortedPage() {
        server.expect(requestTo("https://register.test/v2/instances?searchTerm=Navn%20%2B%20teneste"
                + "&includeInactive=true&page=1&pageSize=25&sort=name"
                + "&requiredEntitlements=urn%3Aissuer%2Bone&requiredEntitlements=urn%3Asecond"))
            .andExpect(header("X-API-KEY", "test-api-key"))
            .andRespond(withSuccess("""
                {"content":[],"page":{"size":25,"number":1,"totalElements":25,"totalPages":1}}
                """, MediaType.APPLICATION_JSON));

        var results = client.searchInstances(new SearchRelyingPartyResource()
            .withSearchTerm("Navn + teneste").withIncludeInactive(true).withPage(1).withOrdering("name")
            .withRequiredEntitlements(List.of("urn:issuer+one", "urn:second")));

        assertTrue(results.content().isEmpty());
        assertEquals(1, results.page().number());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"content\":[]}", "{\"content\":[],\"page\":{}}", "not json"})
    @DisplayName("Should fail rather than show no registrations when search data is missing or unreadable")
    void shouldFailWhenSearchDataIsMissingOrUnreadable(String response) {
        server.expect(requestTo("https://register.test/v2/instances?searchTerm=&includeInactive=false&page=0&pageSize=25&sort=createdMs"))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        assertThrows(UnrecognizedErrorResponseException.class,
            () -> client.searchInstances(new SearchRelyingPartyResource()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}"})
    @DisplayName("Should reject the whole search page when a registration is missing required data")
    void shouldRejectSearchPageWithIncompleteRegistration(String incompleteRegistration) {
        server.expect(requestTo("https://register.test/v2/instances?searchTerm=&includeInactive=false&page=0&pageSize=25&sort=createdMs"))
            .andRespond(withSuccess("""
                {"content":[
                    {"id":"00000000-0000-0000-0000-000000000001","orgno":"123456785",
                     "relyingPartyEntitlements":[],"accessCertificateIds":[],"issuerCertificateIds":[]},
                    %s
                ],"page":{"size":25,"number":0,"totalElements":2,"totalPages":1}}
                """.formatted(incompleteRegistration), MediaType.APPLICATION_JSON));

        assertThrows(UnrecognizedErrorResponseException.class,
            () -> client.searchInstances(new SearchRelyingPartyResource()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"entitlements\":[null]}",
        "{\"entitlements\":[{\"entitlement\":\"urn:first\"},{}]}"})
    @DisplayName("Should distinguish a missing or incomplete role catalog from a catalog with no roles")
    void shouldDistinguishMissingOrIncompleteCatalogFromNoRoles(String response) {
        String url = "https://register.test/v2/entitlements?includeInactive=false";
        server.expect(requestTo(url)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        server.expect(requestTo(url)).andRespond(withSuccess("{\"entitlements\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(UnrecognizedErrorResponseException.class, () -> client.getEntitlements(false));
        assertTrue(client.getEntitlements(false).isEmpty());
    }

    @Test
    @DisplayName("Should retrieve organization and service details with the issuer role")
    void shouldRetrieveOrganizationAndServiceDetailsWithIssuerRole() {
        server.expect(requestTo(REGISTRATION_URL))
            .andExpect(header("X-API-KEY", "test-api-key"))
            .andRespond(withSuccess("""
                {"id":"00000000-0000-0000-0000-000000000001","orgno":"123456785",
                 "organizationName":"Legal name","serviceTradeName":"Service name","isPSB":true,
                 "relyingPartyEntitlements":[{"entitlement":"urn:issuer","displayName":"Issuer",
                                               "credentialIssuerUrl":"https://issuer.test"}],
                 "relyingPartyEaas":[],"accessCertificates":[],"issuerCertificates":[],
                 "createdMs":1700000000000,"lastUpdatedMs":1710000000000,"active":false}
                """, MediaType.APPLICATION_JSON));

        var relyingPartyResource = client.getRelyingParty(RELYING_PARTY_INSTANCE_ID);

        assertAll(
            () -> assertEquals("Service name", relyingPartyResource.tradeName()),
            () -> assertEquals("Legal name", relyingPartyResource.orgName()),
            () -> assertTrue(relyingPartyResource.publicSector()),
            () -> assertEquals(
                List.of(new RelyingPartyEntitlementResource("urn:issuer", "Issuer", "https://issuer.test")),
                relyingPartyResource.relyingPartyEntitlements()));
    }

    @Test
    @DisplayName("Should identify the requested registration in a not-found error when it does not exist")
    void shouldIdentifyRequestedRegistrationWhenItDoesNotExist() {
        server.expect(requestTo(REGISTRATION_URL))
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"not_found\",\"error_description\":\"Missing resource\"}"));

        var failure = assertThrows(RelyingPartyNotFoundException.class, () -> client.getRelyingParty(RELYING_PARTY_INSTANCE_ID));

        assertEquals(RELYING_PARTY_INSTANCE_ID.toString(), failure.getRequested());
        assertInstanceOf(NotFoundException.class, failure.getCause());
    }

    @Test
    @DisplayName("Should fail to retrieve registration details when register service returns no details")
    void shouldFailToRetrieveRegistrationDetailsWhenResponseIsEmpty() {
        server.expect(requestTo(REGISTRATION_URL))
            .andRespond(withSuccess());

        assertThrows(UnrecognizedErrorResponseException.class, () -> client.getRelyingParty(RELYING_PARTY_INSTANCE_ID));
    }

    @Test
    @DisplayName("Should fail to retrieve registration details with the failed operation and its cause when register service is unreachable")
    void shouldFailToRetrieveRegistrationDetailsWhenRegisterServiceIsUnreachable() {
        var connectionFailure = new ResourceAccessException("Connection refused");
        server.expect(requestTo(REGISTRATION_URL))
            .andRespond(request -> {
                throw connectionFailure;
            });

        var failure = assertThrows(AdminServiceException.class, () -> client.getRelyingParty(RELYING_PARTY_INSTANCE_ID));

        assertEquals(
            "Unable to contact register service to retrieve /registrations/" + RELYING_PARTY_INSTANCE_ID,
            failure.getMessage());
        assertSame(connectionFailure, failure.getCause());
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Should report unreadable registrations with the failed operation and cause")
    void shouldReportUnreadableRegistrationsWithFailedOperationAndCause(boolean details) {
        String url = details ? REGISTRATION_URL : "https://register.test/v2/registrations?orgno=123456785";
        server.expect(requestTo(url)).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        var failure = assertThrows(UnrecognizedErrorResponseException.class, () -> {
            if (details) {
                client.getRelyingParty(RELYING_PARTY_INSTANCE_ID);
            } else {
                client.getRegistrations("123456785");
            }
        });

        String operation = details ? "retrieve /registrations/" + RELYING_PARTY_INSTANCE_ID : "retrieve registrations";
        assertEquals("Unable to read response from register service to " + operation, failure.getMessage());
        assertInstanceOf(RestClientException.class, failure.getCause());
    }

    @Test
    @DisplayName("Report register service failure rather than show that the organization has no registrations")
    void shouldReportFailureToRetrieveRegistrations() {
        server.expect(requestTo("https://register.test/v2/registrations?orgno=123456785"))
            .andRespond(withServerError());

        assertThrows(UnrecognizedErrorResponseException.class, () -> client.getRegistrations("123456785"));
    }
}

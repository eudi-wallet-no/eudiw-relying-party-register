package no.idporten.eudiw.rp.register.lookup.service;

import no.idporten.eudiw.rp.register.lookup.service.exception.UnauthorizedRequestException;
import no.idporten.eudiw.rp.register.lookup.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.register.lookup.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.PagedResponse;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyEntitlementResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.entitlement.EntitlementResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.entitlement.EntitlementsResource;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@SpringBootTest
@ActiveProfiles("local-test")
@Import(MockWebServerConfiguration.class)
@DisplayName("When using the lookup service directly")
public class RelyingPartiesServiceTest {

    @Autowired
    private RelyingPartiesService lookupService;

    @Autowired
    private MockWebServer mockWebServer;

    @DisplayName("then the ResponseErrorHandler properly handles valid 401 response")
    @Test
    void testErrorResponseExceptionThrownOnValidBadRequestResponse() {

        MockResponse badRequestMockResponse = new MockResponse().setResponseCode(401);
        mockWebServer.enqueue(badRequestMockResponse);

        assertThrowsExactly(UnauthorizedRequestException.class,
                            () -> lookupService.search(
                                ResourceGenerator.generateSearchForm().toResource()));
    }

    @Test
    @DisplayName("then valid search result responses are properly deserialized")
    void testCorrectDeserializationOfValidSearchResponse() throws Exception {
        List<RelyingPartyResource> generatedContent = ResourceGenerator.generateRelyingPartiesResource();
        Map<String, String> entitlementDisplayNames = generatedContent.stream()
            .flatMap(relyingParty -> relyingParty.relyingPartyEntitlements().stream())
            .map(RelyingPartyEntitlementResource::entitlement)
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(), entitlement -> "Role " + entitlement));
        List<RelyingPartyResource> expectedContent = generatedContent.stream()
            .map(relyingParty -> relyingParty.withPublicSector(null).withRelyingPartyEntitlements(
                relyingParty.relyingPartyEntitlements().stream()
                    .map(entitlement -> new RelyingPartyEntitlementResource(
                        entitlement.entitlement(), entitlementDisplayNames.get(entitlement.entitlement())))
                    .toList()))
            .toList();
        List<RelyingPartyResource> v2Content = expectedContent.stream()
            .map(relyingParty -> relyingParty.withPublicSector(null).withRelyingPartyEntitlements(
                relyingParty.relyingPartyEntitlements().stream()
                    .map(entitlement -> new RelyingPartyEntitlementResource(entitlement.entitlement(), null))
                    .toList()))
            .toList();
        PagedResponse<RelyingPartyResource> expectedSearchResultResource = new PagedResponse<>(
            expectedContent,
            new PagedResponse.PageMetadata(expectedContent.size(), 0, expectedContent.size(), 1)
        );
        PagedResponse<RelyingPartyResource> v2SearchResultResource = new PagedResponse<>(
            v2Content,
            new PagedResponse.PageMetadata(v2Content.size(), 0, v2Content.size(), 1)
        );
        String responseBody = new ObjectMapper().writer().writeValueAsString(v2SearchResultResource);

        MockResponse mockValidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody);
        mockWebServer.enqueue(mockValidResponse);

        EntitlementsResource entitlementCatalog = new EntitlementsResource(entitlementDisplayNames.entrySet().stream()
            .map(entry -> new EntitlementResource(UUID.randomUUID(), entry.getKey(), entry.getValue(), false))
            .toList());
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(new ObjectMapper().writer().writeValueAsString(entitlementCatalog)));

        PagedResponse<RelyingPartyResource> actualSearchResultResource =
            lookupService.searchWithEntitlementDisplayNames(SearchForm.empty().toResource()
                .withSearchTerm("register")
                .withHideSyntheticOrgnos(true)
                .withRequiredEntitlements(List.of("role-one", "role-two"))
                .withPage(2)
                .withPageSize(10)
                .withOrdering("orgno"));
        assertEquals(expectedSearchResultResource, actualSearchResultResource);

        var searchRequest = takeRequest("/v2/instances", "searchTerm", "register");
        assertEquals("GET", searchRequest.getMethod());
        assertEquals("/v2/instances", searchRequest.getRequestUrl().encodedPath());
        assertEquals("register", searchRequest.getRequestUrl().queryParameter("searchTerm"));
        assertEquals("true", searchRequest.getRequestUrl().queryParameter("hideSyntheticOrgnos"));
        assertEquals(List.of("role-one", "role-two"),
            searchRequest.getRequestUrl().queryParameterValues("requiredEntitlements"));
        assertEquals("2", searchRequest.getRequestUrl().queryParameter("page"));
        assertEquals("10", searchRequest.getRequestUrl().queryParameter("pageSize"));
        assertEquals("orgno", searchRequest.getRequestUrl().queryParameter("sort"));

        var entitlementRequest = takeRequest("/v2/entitlements", "includeInactive", "true");
        assertEquals("/v2/entitlements", entitlementRequest.getRequestUrl().encodedPath());
        assertEquals("true", entitlementRequest.getRequestUrl().queryParameter("includeInactive"));
    }

    private RecordedRequest takeRequest(String path, String queryParameter, String queryValue)
        throws InterruptedException {
        RecordedRequest request;
        do {
            request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        } while (request != null &&
            (!path.equals(request.getRequestUrl().encodedPath()) ||
             !queryValue.equals(request.getRequestUrl().queryParameter(queryParameter))));

        assertNotNull(request, "Expected request to " + path + " with " + queryParameter + "=" + queryValue);
        return request;
    }

    @Nested
    @DisplayName("when the lookup service receives unrecognized responses")
    class UnrecognizedErrorResponseHandlingTests {

        static List<MockResponse> invalidErrorResponses() {
            Function<String, MockResponse> getErrorResponse = bodyJson ->
                new MockResponse()
                           .setResponseCode(400)
                           .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                           .setBody(bodyJson);
            Stream<String> invalidErrorResponseJsons =
                Stream.of(
                // unrecognized field names
                "{\"ERROR\": \"foo\",\"error_descriptions\": \"bar\"}",
                // empty body
                "",
                // missing "error" field
                "{\"error_description\": \"foo\"}",
                // missing "error_description" field
                "{\"error\": \"foo\"}",
                // bad field type
                "{\"error\": [\"foo\"],\"error_description\": \"bar\"}"
            );
            return invalidErrorResponseJsons.map(getErrorResponse).toList();
        }

        @ParameterizedTest
        @MethodSource("invalidErrorResponses")
        @DisplayName("then the ResponseErrorHandler handles invalid error response bodies")
        void testUnrecognizedErrorResponseExceptionThrownOnBadResponse(
            MockResponse serverErrorMockResponse) {

            mockWebServer.enqueue(serverErrorMockResponse);

            assertThrowsExactly(UnrecognizedErrorResponseException.class,
                                () -> lookupService.search(
                                    ResourceGenerator.generateSearchForm().toResource()));
        }
    }
}

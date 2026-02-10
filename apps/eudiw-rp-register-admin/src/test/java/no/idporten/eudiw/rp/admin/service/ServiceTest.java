package no.idporten.eudiw.rp.admin.service;

import no.idporten.eudiw.rp.admin.service.exception.UnauthorizedRequestException;
import no.idporten.eudiw.rp.admin.service.exception.UnrecognizedErrorResponseException;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.resource.PagedResponse;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@SpringBootTest
@ActiveProfiles("junit")
@Import(MockWebServerConfiguration.class)
@DisplayName("When using the lookup service directly")
public class ServiceTest {

    @Autowired
    private RelyingPartiesService relyingPartiesService;

    @Autowired
    private MockWebServer mockWebServer;

    @DisplayName("then the ResponseErrorHandler properly handles valid 401 response")
    @Test
    void testErrorResponseExceptionThrownOnValidUnauthorizedRequestResponse() {

        MockResponse badRequestMockResponse = new MockResponse().setResponseCode(401);
        mockWebServer.enqueue(badRequestMockResponse);

        SearchRelyingPartyResource searchResource =
            ResourceGenerator.generateSearchForm().toResource();
        assertThrowsExactly(UnauthorizedRequestException.class,
                            () -> relyingPartiesService.search(searchResource));
    }

    @Test
    @DisplayName("then valid search result responses are properly deserialized")
    void testCorrectDeserializationOfValidSearchResponse() throws Exception {
        List<RelyingPartyResource> expectedSearchResultResource = ResourceGenerator.generateRelyingPartiesResource();
        PagedResponse<RelyingPartyResource> response = ResourceGenerator.generatePageResponse(expectedSearchResultResource);

        String responseBody = new JsonMapper().writer().writeValueAsString(response);

        MockResponse mockValidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody);
        mockWebServer.enqueue(mockValidResponse);

        PagedResponse<RelyingPartyResource> actualSearchResultResource =
                relyingPartiesService.search(
                    SearchForm.empty().toResource());
        assertEquals(expectedSearchResultResource.getFirst(), actualSearchResultResource.content().getFirst());
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
            SearchRelyingPartyResource searchResource =
                ResourceGenerator.generateSearchForm().toResource();
            assertThrowsExactly(UnrecognizedErrorResponseException.class,
                                () -> relyingPartiesService.search(searchResource));
        }
    }
}

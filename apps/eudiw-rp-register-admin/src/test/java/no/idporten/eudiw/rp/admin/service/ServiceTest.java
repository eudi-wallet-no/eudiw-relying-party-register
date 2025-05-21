package no.idporten.eudiw.rp.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.admin.web.resource.SearchForm;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@SpringBootTest
@ActiveProfiles("local-test")
@Import(MockWebServerConfiguration.class)
@DisplayName("When using the lookup service directly")
public class ServiceTest {

    @Autowired
    private RelyingPartiesService relyingPartiesService;

    @Autowired
    private MockWebServer mockWebServer;

    @DisplayName("then the ResponseErrorHandler properly handles valid 401 response")
    @Test
    void testErrorResponseExceptionThrownOnValidBadRequestResponse() {

        MockResponse badRequestMockResponse = new MockResponse().setResponseCode(401);
        mockWebServer.enqueue(badRequestMockResponse);

        assertThrowsExactly(AdminServiceException.class,
                            () -> relyingPartiesService.search(
                                ResourceGenerator.generateSearchForm().toResource()));
    }

    @Test
    @DisplayName("then valid search result responses are properly deserialized")
    void testCorrectDeserializationOfValidSearchResponse() throws Exception {
        RelyingPartiesResource expectedSearchResultResource = ResourceGenerator.generateRelyingPartiesResource();
        String responseBody = new ObjectMapper().writer().writeValueAsString(expectedSearchResultResource);

        MockResponse mockValidResponse =
            new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody);
        mockWebServer.enqueue(mockValidResponse);

        RelyingPartiesResource actualSearchResultResource =
                relyingPartiesService.search(SearchForm.empty().toResource());
        assertEquals(expectedSearchResultResource, actualSearchResultResource);
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

            assertThrowsExactly(AdminServiceException.class,
                                () -> relyingPartiesService.search(
                                    ResourceGenerator.generateSearchForm().toResource()));
        }
    }
}

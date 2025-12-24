package no.idporten.eudiw.rp.admin.service.enhetsregisteretservice;

import no.idporten.eudiw.rp.admin.service.MockWebServerConfiguration;
import no.idporten.eudiw.rp.admin.service.exception.BadRequestException;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.testdata.TestDataGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@Import(MockWebServerConfiguration.class)
@DisplayName("When using the Enhetsregisteret service to obtain sector info for orgnos")
public class EnhetsregisteretServiceTests {

    @Autowired
    private EnhetsregisteretService enhetsregisteretService;

    @Autowired
    private MockWebServer mockWebServer;

    @Nested
    @DisplayName("when Enhetsregisteret sends back non-200 responses ...")
    class EnhetsregisteretServiceResponseErrorHandlerTests {

        @DisplayName("then NotFoundException thrown on 404 and 410 responses from Enhetsregisteret")
        @ParameterizedTest
        @ValueSource(ints = {404, 410})
        void testNotFoundExceptionThrownOn404and410Responses(int status) {
            MockResponse mockResponse = new MockResponse().setResponseCode(status);
            mockWebServer.enqueue(mockResponse);

            String validOrgno = TestDataGenerator.generateValidOrgno();

            assertThrowsExactly(NotFoundException.class,
                                () -> enhetsregisteretService.queryOrgno(validOrgno));
        }

        @DisplayName("then BadRequestException thrown on 400 responses from Enhetsregisteret")
        @Test
        void testBadRequestExceptionThrownOn400Response() {
            MockResponse badRequestMockResponse = new MockResponse().setResponseCode(400);
            mockWebServer.enqueue(badRequestMockResponse);

            String invalidOrgno = "123123124";

            assertThrowsExactly(BadRequestException.class,
                                () -> enhetsregisteretService.queryOrgno(invalidOrgno));
        }

        @DisplayName("then ErrorResponseException thrown on 500 responses from Enhetsregisteret")
        @Test
        void testErrorResponseExceptionThrownOn500Response() {
            MockResponse badRequestMockResponse = new MockResponse().setResponseCode(500);
            mockWebServer.enqueue(badRequestMockResponse);

            String validOrgno = TestDataGenerator.generateValidOrgno();

            assertThrowsExactly(ErrorResponseException.class,
                                () -> enhetsregisteretService.queryOrgno(validOrgno));
        }
    }

    @Nested
    @DisplayName("when Enhetsregisteret sends back success responses ...")
    class GetPublicSectorForOrgnoTests {

        @Test
        @DisplayName("then the result is false if no known public sector code was found")
        public void testPublicSectorIsFalseForNonPublicSectorSectorCodesResponse() {

            String name = TestDataGenerator.generateName();
            String responseBodyJson = """
                { "institusjonellSektorkode": { "kode": "1234,5678" },
                  "navn": "%s"}
            """.formatted(name);

            MockResponse mockResponse =
                new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(responseBodyJson);
            mockWebServer.enqueue(mockResponse);

            String validOrgno = TestDataGenerator.generateValidOrgno();
            EnhetsregisteretService.EnhetsregisteretResponse response =
                enhetsregisteretService.queryOrgno(validOrgno);
            assertAll(
                () -> assertEquals(name, response.name()),
                () -> assertFalse(response.publicSector())
            );
        }

        @Test
        @DisplayName("then the result is true if at least one sector code is known to be public")
        public void testPublicSectorIsTrueWhenAtLeastOneSectorCodeIsKnownToBePublic() {

            String name = TestDataGenerator.generateName();
            String knownPublicSectorCode = "6100";
            String responseBodyJson = """
                { "institusjonellSektorkode": { "kode": "1234,%s,1337" },
                  "navn": "%s"}
            """.formatted(knownPublicSectorCode, name);

            MockResponse mockResponse =
                new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(responseBodyJson);
            mockWebServer.enqueue(mockResponse);

            String validOrgno = TestDataGenerator.generateValidOrgno();
            EnhetsregisteretService.EnhetsregisteretResponse response =
                enhetsregisteretService.queryOrgno(validOrgno);
            assertAll(
                () -> assertEquals(name, response.name()),
                () -> assertTrue(response.publicSector())
            );
        }
    }
}

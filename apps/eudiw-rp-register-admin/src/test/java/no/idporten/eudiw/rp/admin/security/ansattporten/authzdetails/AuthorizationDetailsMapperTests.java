package no.idporten.eudiw.rp.admin.security.ansattporten.authzdetails;

import no.idporten.eudiw.rp.admin.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the authorization_details mapper ...")
public class AuthorizationDetailsMapperTests {

    @Autowired
    private AuthorizationDetailsMapper mapper;

    @Nested
    @DisplayName("to map ansattporten:altinn:resource authorization_details ...")
    class AltinnServiceDetailsMappingTests {

        @Test
        @DisplayName("then valid type, resource, and authorizedParties are all mapped correctly")
        void testAltinnServiceDetailsResponseWithOneReportee() {
            String reporteeName = TestDataGenerator.generateName();
            String reporteeID = TestDataGenerator.generateValidOrgno();
            String resource = TestDataGenerator.generateName();
            AuthorizationDetails.Response.Orgno orgno = new AuthorizationDetails.Response.Orgno(reporteeID, reporteeName);

            List<Map<String, Object>> authorizedPartiesAsListOfStringToObjectMaps =
                List.of(Map.of("orgno", orgno, "name", reporteeName));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:resource",
                    "resource", resource,
                    "authorized_parties", authorizedPartiesAsListOfStringToObjectMaps);

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap));

            AuthorizationDetails.Response.AuthorizedParties expectedAuthorizedParty =
                new AuthorizationDetails.Response.AuthorizedParties(
                    orgno, reporteeName
                );

            AltinnResourceDetails.Response altinnResourceDetailsResponse =
                assertInstanceOf(AltinnResourceDetails.Response.class, response);
            assertEquals(resource, altinnResourceDetailsResponse.getResource());

            assertAll(
                () -> assertEquals("ansattporten:altinn:resource", response.getType()),
                () -> assertNotNull(response.getAuthorizedParties()),
                () -> assertEquals(1, response.getAuthorizedParties().size()),
                () -> assertEquals(expectedAuthorizedParty, response.getAuthorizedParties().getFirst())
            );

        }

        @Test
        @DisplayName("then valid type, resource, and empty authorizedParties are all mapped correctly")
        void testAltinnServiceDetailsResponseWithNoReportees() {

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:resource",
                    "resource", "foo");

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap));

            assertAll(
                () -> assertEquals("ansattporten:altinn:resource", response.getType()),
                () -> assertInstanceOf(AltinnResourceDetails.Response.class, response),
                () -> assertNotNull(response.getAuthorizedParties())
            );
        }

        @Test
        @DisplayName("then unknown keys are ignored")
        void testAltinnServiceResponseWithUnknownKeys() {
            String reporteeName = TestDataGenerator.generateName();
            String reporteeID = TestDataGenerator.generateValidOrgno();
            String resource = TestDataGenerator.generateName();
            AuthorizationDetails.Response.Orgno orgno = new AuthorizationDetails.Response.Orgno(reporteeID, reporteeName);

            List<Map<String, Object>> authorizedPartiesAsListOfStringToObjectMaps =
                    List.of(Map.of("orgno", orgno, "name", reporteeName));

            String misspelledAuthorizedPartiesKey = "authorizedParties";

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:resource",
                    "resource", resource,
                    misspelledAuthorizedPartiesKey, authorizedPartiesAsListOfStringToObjectMaps
                );

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap)
                );

            assertAll(
                () -> assertEquals("ansattporten:altinn:resource", response.getType()),
                () -> assertInstanceOf(AltinnResourceDetails.Response.class, response),
                () -> assertNotNull(response.getAuthorizedParties()),
                () -> assertTrue(response.getAuthorizedParties().isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("to map ansattporten:orgno authorization_details ...")
    class OrgnoDetailsMappingTests {

        @Test
        @DisplayName("then valid type and org are mapped correctly")
        void testOrgnoDetailsResponseWithOrg() {

            String reporteeName = TestDataGenerator.generateName();
            String reporteeID = TestDataGenerator.generateValidOrgno();
            List<Map<String, Object>> authorizedPartiesAsListOfStringToObjectMaps =
                    List.of(Map.of("orgno", new AuthorizationDetails.Response.Orgno(reporteeID, reporteeName), "name", reporteeName));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:orgno",
                    "authorized_parties", authorizedPartiesAsListOfStringToObjectMaps);

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap));

            // NOTE: name is null since ansattporten:orgno responses do not specify name.
            AuthorizationDetails.Response.AuthorizedParties expectedAuthorizedParty =
                new AuthorizationDetails.Response.AuthorizedParties(new AuthorizationDetails.Response.Orgno(
                        reporteeID, reporteeName), reporteeName);

            assertAll(
                () -> assertInstanceOf(OrgnoDetails.Response.class, response),
                () -> assertEquals("ansattporten:orgno", response.getType()),
                () -> assertNotNull(response.getAuthorizedParties()),
                () -> assertEquals(1, response.getAuthorizedParties().size()),
                () -> assertEquals(expectedAuthorizedParty, response.getAuthorizedParties().getFirst())
            );
        }

        @Test
        @DisplayName("then details are mapped correctly when org is missing")
        void testOrgnoDetailsResponseWithoutOrg() {

            Map<String, Object> authzDetailsAsStringToObjectMap = Map.of("type", "ansattporten:orgno");

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap));

            assertAll(
                () -> assertEquals("ansattporten:orgno", response.getType()),
                () -> assertInstanceOf(OrgnoDetails.Response.class, response),
                () -> assertNotNull(response.getAuthorizedParties()),
                () -> assertTrue(response.getAuthorizedParties().isEmpty())
            );
        }

        @Test
        @DisplayName("then details are mapped correctly despite unknown keys")
        void testOrgnoDetailsResponseWithUnknownKeys() {
            Map<String, Object> orgAsStringToObjectMap = Map.of("ID", "foo-orgno");

            String misspelledOrgKey = "orgno";

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:orgno",
                    misspelledOrgKey, orgAsStringToObjectMap
                );

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap)
                );

            assertAll(
                () -> assertInstanceOf(OrgnoDetails.Response.class, response),
                () -> assertNotNull(response.getAuthorizedParties()),
                () -> assertTrue(response.getAuthorizedParties().isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("when there are errors in the authorization_details object ...")
    class ErroneousAuthorizationDetailsMappingTests {
        @Test
        @DisplayName("Invalid authz details error thrown on missing type")
        void testErrorOnMissingType() {
            String reporteeName = TestDataGenerator.generateName();
            String reporteeID = TestDataGenerator.generateValidOrgno();
            String resource = TestDataGenerator.generateName();

            List<Map<String, Object>> authorizedPartiesAsListOfStringToObjectMaps =
                    List.of(Map.of("Orgno", new AuthorizationDetails.Response.Orgno(reporteeID, reporteeName), "ID", reporteeID));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                    Map.of(
                            "resource", resource,
                            "authorized_parties", authorizedPartiesAsListOfStringToObjectMaps);
            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap));
        }

        @Test
        @DisplayName("Invalid authz details error thrown on missing resource")
        void testErrorOnBlankResource() {
            String reporteeName = TestDataGenerator.generateName();
            String reporteeID = TestDataGenerator.generateValidOrgno();
            String blankResource = "";

            List<Map<String, Object>> authorizedPartiesAsListOfStringToObjectMaps =
                    List.of(Map.of("Orgno", new AuthorizationDetails.Response.Orgno(reporteeID, reporteeName), "ID", reporteeID));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:resource",
                    "resource", blankResource,
                    "authorized_parties", authorizedPartiesAsListOfStringToObjectMaps);

            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap));
        }

        @Test
        @DisplayName("Invalid authz details error on authorizedParty with blank orgno")
        void testErrorOnBlankAltinnServiceDetailsReporteeOrgno() {

            String reporteeName = TestDataGenerator.generateName();
            String blankReporteeID = "";
            String resource = "some-resource";

            List<Map<String, Object>> authorizedPartiesAsListOfStringToObjectMaps =
                    List.of(Map.of("Orgno", new AuthorizationDetails.Response.Orgno(blankReporteeID, reporteeName), "ID", blankReporteeID));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:resource",
                    "resource", resource,
                    "authorized_parties", authorizedPartiesAsListOfStringToObjectMaps);

            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap).getAuthorizedParties());
        }

        @Test
        @DisplayName("Invalid authz details error on authorizedParty with blank orgno")
        void testErrorOnBlankOrgnoDetailsOrgOrgno() {

            String reporteeName = TestDataGenerator.generateName();
            String blankReporteeID = "";

            List<Map<String, Object>> orgAsStringToObjectMaps =
                    List.of(Map.of("orgno", new AuthorizationDetails.Response.Orgno(blankReporteeID, reporteeName), "name", reporteeName));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:orgno",
                    "authorized_parties", orgAsStringToObjectMaps);

            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap).getAuthorizedParties());
        }
    }
}

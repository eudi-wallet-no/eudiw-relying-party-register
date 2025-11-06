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
@ActiveProfiles("local-security-test")
@DisplayName("When using the authorization_details mapper ...")
public class AuthorizationDetailsMapperTests {

    @Autowired
    private AuthorizationDetailsMapper mapper;

    @Nested
    @DisplayName("to map ansattporten:altinn:service authorization_details ...")
    class AltinnServiceDetailsMappingTests {

        @Test
        @DisplayName("then valid type, resource, and reportees are all mapped correctly")
        void testAltinnServiceDetailsResponseWithOneReportee() {
            String reporteeName = TestDataGenerator.generateName();
            String reporteeID = TestDataGenerator.generateValidOrgno();
            String resource = TestDataGenerator.generateName();

            List<Map<String, Object>> reporteesAsListOfStringToObjectMaps =
                List.of(Map.of("Name", reporteeName, "ID", reporteeID));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:service",
                    "resource", resource,
                    "reportees", reporteesAsListOfStringToObjectMaps);

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap));

            AuthorizationDetails.Response.Reportee expectedReportee =
                new AuthorizationDetails.Response.Reportee(
                    reporteeID, reporteeName
                );

            AltinnServiceDetails.Response altinnServiceDetailsResponse =
                assertInstanceOf(AltinnServiceDetails.Response.class, response);
            assertEquals(resource, altinnServiceDetailsResponse.getResource());

            assertAll(
                () -> assertEquals("ansattporten:altinn:service", response.getType()),
                () -> assertNotNull(response.getReportees()),
                () -> assertEquals(1, response.getReportees().size()),
                () -> assertEquals(expectedReportee, response.getReportees().getFirst())
            );

        }

        @Test
        @DisplayName("then valid type, resource, and empty reportees are all mapped correctly")
        void testAltinnServiceDetailsResponseWithNoReportees() {

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:service",
                    "resource", "foo");

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap));

            assertAll(
                () -> assertEquals("ansattporten:altinn:service", response.getType()),
                () -> assertInstanceOf(AltinnServiceDetails.Response.class, response),
                () -> assertNotNull(response.getReportees()),
                () -> assertTrue(response.getReportees().isEmpty())
            );
        }

        @Test
        @DisplayName("then unknown keys are ignored")
        void testAltinnServiceResponseWithUnknownKeys() {
            List<Map<String, Object>> reporteesAsListOfStringToObjectMaps =
                List.of(Map.of(
                    "Name", "foo-name",
                    "ID", "foo-orgno"));

            String misspelledReporteesKey = "reportes";

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:service",
                    "resource", "foo",
                    misspelledReporteesKey, reporteesAsListOfStringToObjectMaps
                );

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap)
                );

            assertAll(
                () -> assertEquals("ansattporten:altinn:service", response.getType()),
                () -> assertInstanceOf(AltinnServiceDetails.Response.class, response),
                () -> assertNotNull(response.getReportees()),
                () -> assertTrue(response.getReportees().isEmpty())
            );
        }
    }

    @Nested
    @DisplayName("to map ansattporten:orgno authorization_details ...")
    class OrgnoDetailsMappingTests {

        @Test
        @DisplayName("then valid type and org are mapped correctly")
        void testOrgnoDetailsResponseWithOrg() {
            String reporteeID = TestDataGenerator.generateValidOrgno();

            Map<String, Object> orgAsStringToObjectMap = Map.of("ID", reporteeID);

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:orgno",
                    "org", orgAsStringToObjectMap);

            AuthorizationDetails.Response response =
                assertDoesNotThrow(
                    () -> mapper.asResponse(authzDetailsAsStringToObjectMap));

            // NOTE: name is null since ansattporten:orgno responses do not specify name.
            AuthorizationDetails.Response.Reportee expectedReportee =
                new AuthorizationDetails.Response.Reportee(reporteeID, null);

            assertAll(
                () -> assertInstanceOf(OrgnoDetails.Response.class, response),
                () -> assertEquals("ansattporten:orgno", response.getType()),
                () -> assertNotNull(response.getReportees()),
                () -> assertEquals(1, response.getReportees().size()),
                () -> assertEquals(expectedReportee, response.getReportees().getFirst())
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
                () -> assertNotNull(response.getReportees()),
                () -> assertTrue(response.getReportees().isEmpty())
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
                () -> assertNotNull(response.getReportees()),
                () -> assertTrue(response.getReportees().isEmpty())
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

            List<Map<String, Object>> reporteesAsListOfStringToObjectMaps =
                List.of(Map.of("Name", reporteeName, "ID", reporteeID));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "resource", resource,
                    "reportees", reporteesAsListOfStringToObjectMaps);
            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap));
        }

        @Test
        @DisplayName("Invalid authz details error thrown on missing resource")
        void testErrorOnBlankResource() {
            String reporteeName = TestDataGenerator.generateName();
            String reporteeID = TestDataGenerator.generateValidOrgno();
            String blankResource = "";

            List<Map<String, Object>> reporteesAsListOfStringToObjectMaps =
                List.of(Map.of("Name", reporteeName, "ID", reporteeID));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:service",
                    "resource", blankResource,
                    "reportees", reporteesAsListOfStringToObjectMaps);

            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap));
        }

        @Test
        @DisplayName("Invalid authz details error on reportee with blank orgno")
        void testErrorOnBlankAltinnServiceDetailsReporteeOrgno() {

            String reporteeName = TestDataGenerator.generateName();
            String blankReporteeID = "";
            String resource = "some-resource";

            List<Map<String, Object>> reporteesAsListOfStringToObjectMaps =
                List.of(Map.of("Name", reporteeName, "ID", blankReporteeID));

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:altinn:service",
                    "resource", resource,
                    "reportees", reporteesAsListOfStringToObjectMaps);

            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap).getReportees());
        }

        @Test
        @DisplayName("Invalid authz details error on reportee with blank orgno")
        void testErrorOnBlankOrgnoDetailsOrgOrgno() {

            String reporteeName = TestDataGenerator.generateName();
            String blankReporteeID = "";

            Map<String, Object> orgAsStringToObjectMaps =
                Map.of("Name", reporteeName, "ID", blankReporteeID);

            Map<String, Object> authzDetailsAsStringToObjectMap =
                Map.of(
                    "type", "ansattporten:orgno",
                    "org", orgAsStringToObjectMaps);

            assertThrowsExactly(InvalidAuthorizationDetailsException.class,
                                () -> mapper.asResponse(authzDetailsAsStringToObjectMap).getReportees());
        }
    }
}

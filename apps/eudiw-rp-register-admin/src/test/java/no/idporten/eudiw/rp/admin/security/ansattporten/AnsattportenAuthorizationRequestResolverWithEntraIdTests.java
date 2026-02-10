package no.idporten.eudiw.rp.admin.security.ansattporten;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.web.security.AuthConstants;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenAuthorizationRequestResolver;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetails;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetailsMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@DisplayName("When resolving authz requests with Ansattporten-EntraID enabled")
@TestPropertySource(properties = {"eudiw-admin-web.security.ansattporten.allow-entra-id=true"})
public class AnsattportenAuthorizationRequestResolverWithEntraIdTests {

    @MockitoSpyBean
    @SuppressWarnings("unused")
    private AnsattportenAuthorizationRequestResolver requestResolverSpy;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnsattportenProperties ansattportenProperties;

    @Autowired
    private AuthorizationDetailsMapper authorizationDetailsMapper;

    @BeforeAll
    public static void assertProperties(@Autowired AnsattportenProperties ansattportenProperties) {
        assertTrue(ansattportenProperties.isAllowEntraId());
    }

    @Test
    @DisplayName("then Ansattporten request does not have EntraID parameters when use_entra_id not set")
    public void testRequestResolverAddsAuthzDetailsForAnsattportenRequests()
        throws Exception {
        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get("/oauth2/authorization/ansattporten"));

        OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

        List<AuthorizationDetails.Request> spiedRequestAuthzDetailsAttribute =
            spiedOauthRequest.getAttribute(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER);

        List<AuthorizationDetails.Request> spiedRequestAuthzDetailsParameter =
            new JsonMapper().readValue(
                                  spiedOauthRequest.getAdditionalParameters()
                                                   .get(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER)
                                                   .toString(),
                                  new TypeReference<List<Map<String, Object>>>() { })
                              .stream()
                              .map(authorizationDetailsMapper::asRequest)
                              .toList();

        String acrValuesAttribute = spiedOauthRequest.getAttribute(AuthConstants.ACR_VALUES_PARAMETER);
        String acrValuesParameter = (String) spiedOauthRequest.getAdditionalParameters().get(AuthConstants.ACR_VALUES_PARAMETER);

        assertAll(
            () -> assertTrue(acrValuesAttribute == null || !acrValuesAttribute.contains(AuthConstants.ACR_ENTRAID_VALUE)),
            () -> assertTrue(acrValuesParameter == null || !acrValuesParameter.contains(AuthConstants.ACR_ENTRAID_VALUE)),
            () -> assertEquals(ansattportenProperties.getRequestAuthorizationDetails(), spiedRequestAuthzDetailsAttribute),
            () -> assertEquals(ansattportenProperties.getRequestAuthorizationDetails(), spiedRequestAuthzDetailsParameter)
        );
    }

    @Test
    @DisplayName("then resolver adds EntraID authz details and ACR for Ansattporten-EntraID requests")
    public void testRequestResolverAddsEntraIdAuthzDetailsAndAcrForAnsattportenEntraIdRequests()
        throws Exception {
        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get("/oauth2/authorization/ansattporten?use_entra_id=true"));

        OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

        List<AuthorizationDetails.Request> spiedRequestAuthzDetailsAttribute =
            spiedOauthRequest.getAttribute(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER);

        List<AuthorizationDetails.Request> spiedRequestAuthzDetailsParameter =
            new JsonMapper().readValue(
                                  spiedOauthRequest.getAdditionalParameters()
                                                   .get(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER)
                                                   .toString(),
                                  new TypeReference<List<Map<String, Object>>>() { })
                              .stream()
                              .map(authorizationDetailsMapper::asRequest)
                              .toList();

        String acrValuesAttribute = spiedOauthRequest.getAttribute(AuthConstants.ACR_VALUES_PARAMETER);
        String acrValuesParameter = (String) spiedOauthRequest.getAdditionalParameters().get(AuthConstants.ACR_VALUES_PARAMETER);

        assertAll(
            () -> assertNotNull(acrValuesAttribute),
            () -> assertNotNull(acrValuesParameter),
            () -> assertTrue(acrValuesAttribute.contains(AuthConstants.ACR_ENTRAID_VALUE)),
            () -> assertTrue(acrValuesParameter.contains(AuthConstants.ACR_ENTRAID_VALUE)),
            () -> assertEquals(ansattportenProperties.getEntraIdRequestAuthorizationDetails(),
                               spiedRequestAuthzDetailsAttribute),
            () -> assertEquals(ansattportenProperties.getEntraIdRequestAuthorizationDetails(),
                               spiedRequestAuthzDetailsParameter)
        );
    }

    @Nested
    @TestPropertySource(properties = "eudiw-admin-web.security.ansattporten.allow-synthetic-reportee=true")
    class WithAllowSyntheticReporteesTrue {

        // re-wire properties to active the TestPropertySource override
        @Autowired
        private AnsattportenProperties ansattportenProperties;

        @Test
        @DisplayName("then resolver ignores use_synthetic_reportee if use_entra_id is set")
        public void testRequestResolverIgnoresUseSyntheticReporteeIfUseEntraIdIsSet()
            throws Exception {
            SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
                new SecurityTestUtils.ResultCaptor<>();
            doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

            mockMvc.perform(get("/oauth2/authorization/ansattporten?use_entra_id=true"));

            OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

            List<AuthorizationDetails.Request> spiedRequestAuthzDetailsAttribute =
                spiedOauthRequest.getAttribute(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER);

            List<AuthorizationDetails.Request> spiedRequestAuthzDetailsParameter =
                new JsonMapper().readValue(
                                      spiedOauthRequest.getAdditionalParameters()
                                                       .get(AuthConstants.AUTHORIZATION_DETAILS_PARAMETER)
                                                       .toString(),
                                      new TypeReference<List<Map<String, Object>>>() { })
                                  .stream()
                                  .map(authorizationDetailsMapper::asRequest)
                                  .toList();

            String acrValuesAttribute = spiedOauthRequest.getAttribute(AuthConstants.ACR_VALUES_PARAMETER);
            String acrValuesParameter = (String) spiedOauthRequest.getAdditionalParameters().get(AuthConstants.ACR_VALUES_PARAMETER);

            assertAll(
                () -> assertNotNull(acrValuesAttribute),
                () -> assertNotNull(acrValuesParameter),
                () -> assertTrue(acrValuesAttribute.contains(AuthConstants.ACR_ENTRAID_VALUE)),
                () -> assertTrue(acrValuesParameter.contains(AuthConstants.ACR_ENTRAID_VALUE)),
                () -> assertEquals(ansattportenProperties.getEntraIdRequestAuthorizationDetails(),
                                   spiedRequestAuthzDetailsAttribute),
                () -> assertEquals(ansattportenProperties.getEntraIdRequestAuthorizationDetails(),
                                   spiedRequestAuthzDetailsParameter)
            );
        }
    }
}

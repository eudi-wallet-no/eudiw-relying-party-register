package no.idporten.eudiw.rp.admin.security.ansattporten;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.web.security.AuthConstants;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenAuthorizationRequestResolver;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetails;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetailsMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.ActiveProfiles;
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

@SpringBootTest(properties = "eudiw-admin-web.security.ansattporten.allow-synthetic-reportee=true")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@DisplayName("When resolving authz requests with synthetic reportees ENABLED")
public class AnsattportenAuthorizationRequestResolverWithSyntheticReporteesTests {

    @MockitoSpyBean
    @SuppressWarnings("unused")
    private AnsattportenAuthorizationRequestResolver requestResolverSpy;

    @Autowired
    private AuthorizationDetailsMapper authorizationDetailsMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnsattportenProperties ansattportenProperties;

    @BeforeAll
    public static void assertProperties(@Autowired AnsattportenProperties ansattportenProperties) {
        assertTrue(ansattportenProperties.isAllowSyntheticReportee());
    }

    @Test
    @DisplayName("then resolver adds authz details for Ansattporten requests without use_synthetic_reportee")
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

        assertAll(
            () -> assertEquals(ansattportenProperties.getRequestAuthorizationDetails(), spiedRequestAuthzDetailsAttribute),
            () -> assertEquals(ansattportenProperties.getRequestAuthorizationDetails(), spiedRequestAuthzDetailsParameter)
        );
    }


    @Test
    @DisplayName("then resolver does not add authorization_details")
    public void testRequestResolverDoesNotAddAuthzDetailsForAnsattportenRequestsWithUseSyntheticReporteeTrue()
        throws Exception {

        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get("/oauth2/authorization/ansattporten?use_synthetic_reportee=true"));

        OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

        assertAll(
            () -> assertNotNull(spiedOauthRequest),
            () -> assertFalse(spiedOauthRequest.getAttributes().containsKey(
                AuthConstants.AUTHORIZATION_DETAILS_PARAMETER)),
            () -> assertFalse(spiedOauthRequest.getAdditionalParameters().containsKey(
                AuthConstants.AUTHORIZATION_DETAILS_PARAMETER))
        );
    }
}

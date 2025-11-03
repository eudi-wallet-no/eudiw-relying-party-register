package no.idporten.eudiw.rp.admin.security.ansattporten;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.admin.web.security.OAuth2Constants;
import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenAuthorizationRequestResolver;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.RequestAuthorizationDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "eudiw-admin-web.security.ansattporten.allow-synthetic-reportee=true")
@AutoConfigureMockMvc
@ActiveProfiles("local-security-test")
@DisplayName("When resolving authz requests with synthetic reportees ENABLED")
public class AnsattportenAuthorizationRequestResolverTests {

    @MockitoSpyBean
    @SuppressWarnings("unused")
    private AnsattportenAuthorizationRequestResolver requestResolverSpy;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnsattportenProperties ansattportenProperties;

    @Test
    @DisplayName("then resolver adds authz details for Ansattporten requests without use_synthetic_reportee")
    public void testRequestResolverAddsAuthzDetailsForAnsattportenRequests()
        throws Exception {
        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get("/oauth2/authorization/ansattporten"));

        OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

        RequestAuthorizationDetails spiedRequestAuthzDetailsAttribute =
            spiedOauthRequest.getAttribute(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER);

        RequestAuthorizationDetails spiedRequestAuthzDetailsParameter = new ObjectMapper().readValue(
            spiedOauthRequest.getAdditionalParameters()
                             .get(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)
                             .toString(),
            RequestAuthorizationDetails[].class)[0];

        assertAll(
            () -> assertEquals(ansattportenProperties.authorizationDetails(), spiedRequestAuthzDetailsAttribute),
            () -> assertEquals(ansattportenProperties.authorizationDetails(), spiedRequestAuthzDetailsParameter)
        );
    }

    @Nested
    @DisplayName("when use-synthetic-reportee=true")
    class WithUseSyntheticReporteeTrue {
        @MockitoSpyBean
        @SuppressWarnings("unused")
        private AnsattportenProperties ansattportenPropertiesSpy;

        @Test
        @DisplayName("then resolver does not add authorization_details")
        public void testRequestResolverDoesNotAddAuthzDetailsForAnsattportenRequestsWithUseSyntheticReporteeTrue()
            throws Exception {
            when(ansattportenPropertiesSpy.allowSyntheticReportee()).thenReturn(true);

            SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
                new SecurityTestUtils.ResultCaptor<>();
            doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

            mockMvc.perform(get("/oauth2/authorization/ansattporten?use_synthetic_reportee=true"));

            OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

            assertAll(
                () -> assertNotNull(spiedOauthRequest),
                () -> assertFalse(spiedOauthRequest.getAttributes().containsKey(
                    OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)),
                () -> assertFalse(spiedOauthRequest.getAdditionalParameters().containsKey(
                    OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER))
            );
        }
    }

    @Test
    @DisplayName("then request resolver does not add authorization_details for non-Ansattporten requests")
    public void testRequestResolverDoesNotAddAuthzDetailsForNonAnsattportenRequests()
        throws Exception {
        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get("/oauth2/authorization/entra"));

        OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

        assertAll(
            () -> assertNotNull(spiedOauthRequest),
            () -> assertFalse(spiedOauthRequest.getAttributes().containsKey(
                OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)),
            () -> assertFalse(spiedOauthRequest.getAdditionalParameters().containsKey(
                OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER))
        );
    }
}

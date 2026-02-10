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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@DisplayName("When resolving authz requests with default config (allowSyntheticReportee = false)")
public class AnsattportenAuthorizationRequestResolverTests {

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
    public static void assertPropertiesBeforeTests(@Autowired AnsattportenProperties ansattportenProperties) {
        assertFalse(ansattportenProperties.isAllowSyntheticReportee());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/oauth2/authorization/ansattporten?use_synthetic_reportee=true",
                            "/oauth2/authorization/ansattporten"})
    @DisplayName("then resolver ignores use_synthetic_reportee and always adds authz details for Ansattporten requests")
    public void testRequestResolverIgnoresUseSyntheticReporteeForAnsattportenRequests(String requestUri)
        throws Exception {
        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get(requestUri));

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
    @DisplayName("then resolver ignores use_entra_id for Ansattporten requests")
    public void testRequestResolverIgnoresUseEntraIdFOrAnsattportenRequests()
        throws Exception {

        String requestUri = "/oauth2/authorization/ansattporten?use_entra_id=true";

        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get(requestUri));

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
    @DisplayName("then resolver adds basic authz parameters to Ansattporten requests")
    public void testRequestResolverAddsBasicAuthzParameters() throws Exception {

        String requestUri = "/oauth2/authorization/ansattporten";

        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get(requestUri));

        OAuth2AuthorizationRequest spiedOauthRequest = oauthRequestCaptor.getResult();

        String acrValuesAttribute = spiedOauthRequest.getAttribute(AuthConstants.ACR_VALUES_PARAMETER);
        String acrValuesParameter = (String) spiedOauthRequest.getAdditionalParameters().get(AuthConstants.ACR_VALUES_PARAMETER);

        String promptAttribute = spiedOauthRequest.getAttribute(AuthConstants.PROMPT_PARAMETER);
        String promptParameter = (String) spiedOauthRequest.getAdditionalParameters().get(AuthConstants.PROMPT_PARAMETER);

        String codeVerifierAttribute = spiedOauthRequest.getAttribute(AuthConstants.CODE_VERIFIER_PARAMETER);

        String codeChallengeParameter =
            (String) spiedOauthRequest.getAdditionalParameters()
                                      .get(AuthConstants.CODE_CHALLENGE_PARAMETER);
        String codeChallengeMethodParameter =
            (String) spiedOauthRequest.getAdditionalParameters()
                                      .get(AuthConstants.CODE_CHALLENGE_METHOD_PARAMETER);

        assertAll(
            () -> assertEquals(AuthConstants.ACR_SUBSTANTIAL_VALUE, acrValuesAttribute),
            () -> assertEquals(AuthConstants.ACR_SUBSTANTIAL_VALUE, acrValuesParameter),

            () -> assertEquals(AuthConstants.PROMPT_LOGIN_VALUE, promptAttribute),
            () -> assertEquals(AuthConstants.PROMPT_LOGIN_VALUE, promptParameter),

            () -> assertNotNull(codeVerifierAttribute),

            () -> assertNotNull(codeChallengeParameter),
            () -> assertEquals(AuthConstants.CODE_CHALLENGE_METHOD_S256, codeChallengeMethodParameter)
        );
    }
}

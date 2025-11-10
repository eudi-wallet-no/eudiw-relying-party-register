package no.idporten.eudiw.rp.admin.security.ansattporten;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.web.security.OAuth2Constants;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-security-test")
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
            spiedOauthRequest.getAttribute(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER);

        List<AuthorizationDetails.Request> spiedRequestAuthzDetailsParameter =
            new ObjectMapper().readValue(
                spiedOauthRequest.getAdditionalParameters()
                                 .get(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)
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
            spiedOauthRequest.getAttribute(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER);

        List<AuthorizationDetails.Request> spiedRequestAuthzDetailsParameter =
            new ObjectMapper().readValue(
                                  spiedOauthRequest.getAdditionalParameters()
                                                   .get(OAuth2Constants.OAUTH2_AUTHORIZATION_DETAILS_PARAMETER)
                                                   .toString(),
                                  new TypeReference<List<Map<String, Object>>>() { })
                              .stream()
                              .map(authorizationDetailsMapper::asRequest)
                              .toList();

        String acrValuesAttribute = spiedOauthRequest.getAttribute("acr_values");
        String acrValuesParameter = (String) spiedOauthRequest.getAdditionalParameters().get("acr_values");

        assertAll(
            () -> assertTrue(acrValuesAttribute == null || !acrValuesAttribute.contains("entraid")),
            () -> assertTrue(acrValuesParameter == null || !acrValuesParameter.contains("entraid")),
            () -> assertEquals(ansattportenProperties.getRequestAuthorizationDetails(), spiedRequestAuthzDetailsAttribute),
            () -> assertEquals(ansattportenProperties.getRequestAuthorizationDetails(), spiedRequestAuthzDetailsParameter)
        );
    }
}

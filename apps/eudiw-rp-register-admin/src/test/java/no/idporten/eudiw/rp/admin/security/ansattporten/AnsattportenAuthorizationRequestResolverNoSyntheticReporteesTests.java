package no.idporten.eudiw.rp.admin.security.ansattporten;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.syntheticreportees.SyntheticReporteeProvider;
import no.idporten.eudiw.rp.admin.web.security.OAuth2Constants;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenAuthorizationRequestResolver;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.RequestAuthorizationDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "eudiw-admin-web.security.ansattporten.allow-synthetic-reportee=false")
@AutoConfigureMockMvc
@ActiveProfiles("local-security-test")
@DisplayName("When resolving authz requests with synthetic reportees DISABLED")
public class AnsattportenAuthorizationRequestResolverNoSyntheticReporteesTests {

    @MockitoSpyBean
    @SuppressWarnings("unused")
    private AnsattportenAuthorizationRequestResolver requestResolverSpy;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnsattportenProperties ansattportenProperties;

    @ParameterizedTest
    @ValueSource(strings = {"/oauth2/authorization/ansattporten?use_synthetic_reportee=true",
                            "/oauth2/authorization/ansattporten"})
    @DisplayName("then resolver ignores use_synthetic_reportee and always adds authz details for Ansattporten requests")
    public void testRequestResolverIgnoresUseSyntheticReporteeForForAnsattportenRequests(String requestUri)
        throws Exception {
        SecurityTestUtils.ResultCaptor<OAuth2AuthorizationRequest> oauthRequestCaptor =
            new SecurityTestUtils.ResultCaptor<>();
        doAnswer(oauthRequestCaptor).when(requestResolverSpy).resolve(any());

        mockMvc.perform(get(requestUri));

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
}

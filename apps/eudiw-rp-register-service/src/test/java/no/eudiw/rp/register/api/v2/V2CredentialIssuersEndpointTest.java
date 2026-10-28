package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.service.CredentialIssuersService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V2CredentialIssuersEndpointTest {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_KEY = "junit-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CredentialIssuersService credentialIssuersService;

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should return registered issuer URLs through V2 and preserve V1")
    void shouldReturnRegisteredIssuerUrlsThroughV2AndPreserveV1(String path) throws Exception {
        when(credentialIssuersService.getRegisteredCredentialIssuerUrls())
            .thenReturn(Set.of("https://issuer.example/Issuer", "https://issuer.example/issuer"));

        mockMvc.perform(get(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.credential_issuer_urls", containsInAnyOrder(
                "https://issuer.example/Issuer", "https://issuer.example/issuer")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should return an empty issuer list through V2 and preserve V1")
    void shouldReturnAnEmptyIssuerListThroughV2AndPreserveV1(String path) throws Exception {
        when(credentialIssuersService.getRegisteredCredentialIssuerUrls()).thenReturn(Set.of());

        mockMvc.perform(get(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.credential_issuer_urls").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should reject issuer reads without an API key")
    void shouldRejectIssuerReadsWithoutAnApiKey(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());

        verifyNoInteractions(credentialIssuersService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should reject issuer reads with an invalid API key")
    void shouldRejectIssuerReadsWithAnInvalidApiKey(String path) throws Exception {
        mockMvc.perform(get(path).header(API_KEY_HEADER, "invalid-api-key"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(credentialIssuersService);
    }
}

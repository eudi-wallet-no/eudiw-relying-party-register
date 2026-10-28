package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import org.springframework.dao.DataAccessResourceFailureException;
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
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should return the URLs of registered credential issuers")
    void shouldReturnTheUrlsOfRegisteredCredentialIssuers(String path) throws Exception {
        when(relyingPartyInstanceRepository.findActiveCredentialIssuerUrls())
            .thenReturn(Set.of("https://issuer.example/Issuer", "https://issuer.example/issuer"));

        mockMvc.perform(get(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.credential_issuer_urls", containsInAnyOrder(
                "https://issuer.example/Issuer", "https://issuer.example/issuer")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should return an empty list when no credential issuers are registered")
    void shouldReturnAnEmptyListWhenNoCredentialIssuersAreRegistered(String path) throws Exception {
        when(relyingPartyInstanceRepository.findActiveCredentialIssuerUrls()).thenReturn(Set.of());

        mockMvc.perform(get(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.credential_issuer_urls").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should report an error instead of an empty issuer list when registered issuers cannot be read")
    void shouldReportAnErrorInsteadOfAnEmptyIssuerListWhenRegisteredIssuersCannotBeRead(String path) throws Exception {
        when(relyingPartyInstanceRepository.findActiveCredentialIssuerUrls())
            .thenThrow(new DataAccessResourceFailureException("Issuer register database unavailable"));

        mockMvc.perform(get(path).header(API_KEY_HEADER, API_KEY))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("server_error"))
            .andExpect(jsonPath("$.credential_issuer_urls").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should reject issuer reads without an API key")
    void shouldRejectIssuerReadsWithoutAnApiKey(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());

        verifyNoInteractions(relyingPartyInstanceRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v2/credential-issuers", "/v1/credential-issuers"})
    @DisplayName("Should reject issuer reads with an invalid API key")
    void shouldRejectIssuerReadsWithAnInvalidApiKey(String path) throws Exception {
        mockMvc.perform(get(path).header(API_KEY_HEADER, "invalid-api-key"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(relyingPartyInstanceRepository);
    }
}

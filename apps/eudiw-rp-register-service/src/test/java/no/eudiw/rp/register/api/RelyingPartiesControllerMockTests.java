package no.eudiw.rp.register.api;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.RelyingPartyOrdering;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import no.eudiw.rp.register.data.service.RelyingPartyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.*;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the Relying Parties API with mock service")
@ActiveProfiles("test")
public class RelyingPartiesControllerMockTests {

    private static final String X_API_KEY_HEADER = "X-API-KEY";
    private static final String VALID_API_KEY = "junit-api-key";

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartyService mockRpService;

    @Nested
    @DisplayName("When searching for relying parties ...")
    class SearchTests {

        @Test
        @DisplayName("then default value of required_entitlements is used in deserialization")
        public void testRequiredEntitlementsDefaultValueRespected() throws Exception {

            String searchTerm = generateName();
            boolean includeInactive = true;
            String searchJson = "{\"search_term\": \"%s\", \"include_inactive\": %s}"
                                    .formatted(searchTerm, includeInactive);

            SearchRelyingPartyResource expectedSearchResource =
                new SearchRelyingPartyResource(searchTerm).withIncludeInactive(includeInactive);

            mockMvc.perform(post("/v1/rp/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(searchJson))
                   .andExpect(status().isOk());

            verify(mockRpService).searchRelyingParties(expectedSearchResource);
        }
    }
}

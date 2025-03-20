package no.eudiw.rp.register.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
import static no.eudiw.rp.register.testdata.EntityGenerator.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static no.eudiw.rp.register.testdata.ResourceGenerator.generateCreateRelyingPartyResource;
import static no.eudiw.rp.register.testdata.ResourceGenerator.generateEditRelyingPartyResource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("When using the relying parties API with invalid resources ...")
@AutoConfigureMockMvc
public class ResourceValidationTests {

    @Autowired
    private RelyingPartyRepository relyingPartyRepository;

    @Autowired
    private MockMvc mockMvc;

    private static final String X_API_KEY_HEADER = "X-API-KEY";
    private static final String VALID_API_KEY = "junit-api-key";

    private ResultActions mvcPerform(MockHttpServletRequestBuilder builder,
                                     Object resource) throws Exception {
        String content =
            resource != null ? new ObjectMapper().writeValueAsString(resource) : "";
        return mockMvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_API_KEY_HEADER, VALID_API_KEY)
                        .content(content));
    }

    @Nested
    @DisplayName("When passing create resources to the create endpoint ...")
    class CreateRelyingPartyResourceValidationTests {

        @Test
        @DisplayName("then request rejected if orgno is invalid")
        void testInvalidOrgnoInCreateResource() throws Exception {

            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();
            resource.setOrgNr(generateInvalidOrgno());

            mvcPerform(post("/v1/rp"), resource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description", containsString("invalid_orgnr")));
        }
    }

    @Nested
    @DisplayName("When passing edit resources to the edit endpoint ...")
    class EditRelyingPartyResourceValidationTests {

        @Test
        @DisplayName("then request rejected if name is invalid")
        void testInvalidNameEditResource() throws Exception {
            RelyingParty relyingPartyIn = relyingPartyRepository.save(generateRelyingPartyNoId());
            UUID id = relyingPartyIn.getId();

            EditRelyingPartyResource resource = generateEditRelyingPartyResource();
            resource.setName("<script>Digdir</script>");

            mvcPerform(put("/v1/rp/" + id), resource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description", containsString("invalid_name,unsane_string")));
        }
    }

    @Nested
    @DisplayName("When passing search resources to the search endpoint ...")
    class SearchRelyingPartyResourceValidationTests {

        @Test
        @DisplayName("then request rejected if orgno is null")
        void testNullOrgnoInSearchResource() throws Exception {
            SearchRelyingPartyResource searchResource =
                new SearchRelyingPartyResource(null, true, true);

            mvcPerform(post("/v1/rp/search"), searchResource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description", containsString("null_orgno")));
        }
    }

    @Nested
    @DisplayName("When passing resources with properties unknown to the API ...")
    class UnknownPropertiesTests {
        @Test
        void testBadResourceAtEndpoint() throws Exception {
            EditRelyingPartyResource editResource =
                generateEditRelyingPartyResource();

            mvcPerform(post("/v1/rp/search"), editResource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
        }

        @Test
        void testUnknownPropertiesInResource() throws Exception {
            CreateRelyingPartyResource faultyCreateResource =
                new CreateRelyingPartyResource() {
                    @JsonProperty(value = "unknown_property", defaultValue = "foo")
                    @SuppressWarnings("unused")
                    String _unknownProperty;
                };

            mvcPerform(post("/v1/rp"), faultyCreateResource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
        }
    }

    @DisplayName("When querying deletion of a UUID")
    @Test
    void testDeleteBadUuid() throws Exception {
        String invalidId = "invalid-id";
        mvcPerform(delete("/v1/rp/" + invalidId), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"));
    }
}

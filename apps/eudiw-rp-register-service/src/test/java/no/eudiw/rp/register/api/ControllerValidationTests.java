package no.eudiw.rp.register.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.api.resource.relyingparty.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.resource.relyingparty.EditRelyingPartyResource;
import no.eudiw.rp.register.api.resource.relyingparty.SearchRelyingPartyResource;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
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

import static no.eudiw.rp.register.testdata.ResourceGenerator.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the relying parties API with invalid resources ...")
@AutoConfigureMockMvc
public class ControllerValidationTests {

    @Autowired
    private RelyingPartyInstanceRepository relyingPartyRepository;

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
    @DisplayName("When using the create endpoint ...")
    class CreateEndpointValidationTests {

        @Test
        @DisplayName("then validation is properly applied to create resource")
        void testInvalidOrgnoInCreateResource() throws Exception {

            CreateRelyingPartyResource resource =
                generateCreateRelyingPartyResource()
                    .withOrgNr(generateInvalidOrgno());

            mvcPerform(post("/v1/rp"), resource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description", containsString("invalid_orgnr")));
        }
    }

    @Nested
    @DisplayName("When using the edit endpoint ...")
    class EditEndpointValidationTests {

        @Test
        @DisplayName("then validation is properly applied to edit resource")
        void testUnsaneNameEditResource() throws Exception {
            RelyingPartyInstance relyingPartyIn = relyingPartyRepository.save(generateRelyingParty());
            UUID id = relyingPartyIn.getId();

            String invalidName ="<script>Digdir</script>";
            EditRelyingPartyResource resource =
                generateEditRelyingPartyResource().withTradeName(invalidName);

            mvcPerform(put("/v1/rp/" + id), resource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description", containsString("unsane_string")));
        }
    }

    @Nested
    @DisplayName("When using the search endpoint ...")
    class SearchEndpointValidationTests {

        @Test
        @DisplayName("then validation is properly applied to search resource")
        void testOptionalParametersCanBeNull() throws Exception {
            SearchRelyingPartyResource resource = new SearchRelyingPartyResource("$fornothing");

            mvcPerform(post("/v1/rp/search"), resource)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description", containsString("unsane_string")));
        }
    }

    @Nested
    @DisplayName("When using the delete endpoint ...")
    class DeleteEndpointValidationTests {

        @DisplayName("then validation is properly applied to the delete ID")
        @Test
        void testDeleteBadUuid() throws Exception {
            String invalidId = "invalid-id";
            mvcPerform(delete("/v1/rp/" + invalidId), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
        }
    }

    @Nested
    @DisplayName("When using the get endpoint ...")
    class GetEndpointValidationTests {

        @DisplayName("then validation is properly applied to the get ID")
        @Test
        void testGetadUuid() throws Exception {
            String invalidId = "invalid-id";
            mvcPerform(get("/v1/rp/" + invalidId), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
        }
    }
}

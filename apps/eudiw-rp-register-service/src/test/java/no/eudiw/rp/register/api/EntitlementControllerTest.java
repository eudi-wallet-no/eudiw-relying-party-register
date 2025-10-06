package no.eudiw.rp.register.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.eudiw.rp.register.api.resource.CreateEntitlementResource;
import no.eudiw.rp.register.api.resource.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.resource.EditEntitlementResource;
import no.eudiw.rp.register.data.service.EntitlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static no.eudiw.rp.register.testdata.ResourceGenerator.generateCreateRelyingPartyResource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the Entitlement API")
@ActiveProfiles("test")
class EntitlementControllerTest {

    public static final String X_API_KEY_HEADER = "X-API-KEY";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntitlementService entitlementService;

    @DisplayName("When using the Entitlement API with valid API key")
    @Nested
    class APITests {

        public static final String VALID_API_KEY = "junit-api-key";

        @Test
        void testCreateEntitlement() throws Exception {
            CreateEntitlementResource resource = new CreateEntitlementResource("entitlement99", "entitlement99", "access");

            ObjectWriter ow = new ObjectMapper().writer();
            String json = ow.writeValueAsString(resource);

            mockMvc.perform(post("/v1/entitlement")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                    .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entitlement").value("entitlement99"))
                .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        void testEditEntitlement() throws Exception {
            CreateEntitlementResource createResource = new CreateEntitlementResource("entitlement98", "entitlement98", "access");

            ObjectWriter ow = new ObjectMapper().writer();
            String createJson = ow.writeValueAsString(createResource);

            mockMvc.perform(post("/v1/entitlement")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                    .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entitlement").value("entitlement98"))
                .andExpect(jsonPath("$.active").value(true));

            EditEntitlementResource editResource = new EditEntitlementResource(false);

            String editJson = ow.writeValueAsString(editResource);

            mockMvc.perform(put("/v1/entitlement/entitlement98")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                    .content(editJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entitlement").value("entitlement98"))
                .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        void testGetAllEntitlement() throws Exception {
            CreateEntitlementResource createResource = new CreateEntitlementResource("entitlement97", "entitlement97", "access");

            ObjectWriter ow = new ObjectMapper().writer();
            String createJson = ow.writeValueAsString(createResource);

            mockMvc.perform(post("/v1/entitlement")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_API_KEY_HEADER, VALID_API_KEY)
                    .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entitlement").value("entitlement97"))
                .andExpect(jsonPath("$.active").value(true));

            mockMvc.perform(get("/v1/entitlement")
                    .accept(MediaType.APPLICATION_JSON)
                    .header(X_API_KEY_HEADER, VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entitlements").exists())
                .andExpect(jsonPath("$.entitlements").isArray());
        }
    }
}

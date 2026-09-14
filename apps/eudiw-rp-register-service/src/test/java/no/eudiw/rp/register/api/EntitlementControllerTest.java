package no.eudiw.rp.register.api;

import no.eudiw.rp.register.data.entity.Entitlement;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@DisplayName("When using the Entitlement API")
class EntitlementControllerTest {

    private static final String X_API_KEY_HEADER = "X-API-KEY";
    private static final String VALID_API_KEY = "junit-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Test
    void retrievesFlywayManagedEntitlements() throws Exception {
        mockMvc.perform(get("/v1/entitlement")
                .accept(MediaType.APPLICATION_JSON)
                .header(X_API_KEY_HEADER, VALID_API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entitlements").isArray());
    }

    @Test
    @DisplayName("without includeInactive only returns active entitlements")
    void defaultBehaviorReturnsOnlyActiveEntitlements() throws Exception {
        String inactiveEntitlement = "urn:eudiw:test:" + UUID.randomUUID();
        entitlementRepository.save(new Entitlement(inactiveEntitlement, false, inactiveEntitlement, "access"));

        mockMvc.perform(get("/v1/entitlement")
                .accept(MediaType.APPLICATION_JSON)
                .header(X_API_KEY_HEADER, VALID_API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entitlements[?(@.entitlement=='" + inactiveEntitlement + "')]").doesNotExist())
            .andExpect(jsonPath("$.entitlements[*].active", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    @DisplayName("with includeInactive=true returns both active and inactive entitlements")
    void includeInactiveTrueReturnsInactiveEntitlementsToo() throws Exception {
        String inactiveEntitlement = "urn:eudiw:test:" + UUID.randomUUID();
        entitlementRepository.save(new Entitlement(inactiveEntitlement, false, inactiveEntitlement, "access"));

        mockMvc.perform(get("/v1/entitlement")
                .queryParam("includeInactive", "true")
                .accept(MediaType.APPLICATION_JSON)
                .header(X_API_KEY_HEADER, VALID_API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entitlements[?(@.entitlement=='" + inactiveEntitlement + "')]").exists());
    }

    @Test
    void rejectsEntitlementAdministration() throws Exception {
        mockMvc.perform(post("/v1/entitlement")
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_API_KEY_HEADER, VALID_API_KEY)
                .content("{}"))
            .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/v1/entitlement/entitlement")
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_API_KEY_HEADER, VALID_API_KEY)
                .content("{}"))
            .andExpect(status().isNotFound());
    }
}

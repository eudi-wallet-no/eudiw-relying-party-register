package no.idporten.eudiw.credential.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("junit")
@SpringBootTest
@AutoConfigureMockMvc
class CredentialRegisteryApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should fetch available credential types from the same address as the API documentation")
    void shouldFetchAvailableCredentialTypesFromSameAddressAsApiDocumentation() throws Exception {
        mockMvc.perform(get("http://bevisregister.example:443/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("/"));
    }

    @Test
    @DisplayName("Verify that Spring contexts are loaded without exceptions")
    void contextLoads() {
    }
}
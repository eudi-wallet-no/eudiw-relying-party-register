package no.idporten.eudiw.rp.register.lookup.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the Lookup API")
@ActiveProfiles("test")
public class LookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("the request does not required an API key and is redirected to the Swagger UI")
    @Test
    void lookupRelyingParties() throws Exception {
        mockMvc.perform(get("/v1/rp/lookup")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

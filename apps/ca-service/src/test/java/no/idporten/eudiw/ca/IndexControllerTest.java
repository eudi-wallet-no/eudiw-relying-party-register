package no.idporten.eudiw.ca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@DisplayName("When accessing the root path")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("then redirect to the Swagger UI")
    @Test
    void testRedirectToSwaggerUI() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui.html"));
    }

}

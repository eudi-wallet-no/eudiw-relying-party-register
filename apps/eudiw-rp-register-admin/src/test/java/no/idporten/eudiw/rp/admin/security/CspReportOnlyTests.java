package no.idporten.eudiw.rp.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("junit")
@AutoConfigureMockMvc
class CspReportOnlyTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reportOnlyOnPublicAndProtectedResponses() throws Exception {
        for (String path : new String[]{"/login", "/css/styles.css", "/search"}) {
            mockMvc.perform(get(path))
                .andExpect(header().string("Content-Security-Policy-Report-Only",
                    allOf(containsString("script-src 'self'"),
                        containsString("style-src-attr 'none'"),
                        containsString("report-uri https://csp-report.digdir.no/api/reports"),
                        not(containsString("unsafe-inline")))))
                .andExpect(header().doesNotExist("Content-Security-Policy"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }

    @Test
    void loginRendersAndStylesAreAccessible() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/css/styles.css")).andExpect(status().isOk());
    }
}

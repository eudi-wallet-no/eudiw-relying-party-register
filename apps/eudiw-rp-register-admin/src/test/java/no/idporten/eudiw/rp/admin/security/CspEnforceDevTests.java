package no.idporten.eudiw.rp.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "eudiw-admin-web.security.enabled=false",
    "eudiw-admin-web.csp.mode=enforce"
})
@ActiveProfiles("junit")
@AutoConfigureMockMvc
class CspEnforceDevTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enforceHeaderOnDevLoginAndStaticResources() throws Exception {
        for (String path : new String[]{"/login", "/css/styles.css"}) {
            mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                    containsString("script-src-attr 'none'")))
                .andExpect(header().doesNotExist("Content-Security-Policy-Report-Only"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }
}

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

@SpringBootTest(properties = "eudiw-admin-web.csp.mode=enforce")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
class CspEnforceTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enforceHeaderOnProductionSecurityChain() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Security-Policy",
                containsString("frame-ancestors 'none'")))
            .andExpect(header().doesNotExist("Content-Security-Policy-Report-Only"));
    }
}

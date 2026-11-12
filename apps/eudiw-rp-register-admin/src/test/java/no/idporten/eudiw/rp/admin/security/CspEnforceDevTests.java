package no.idporten.eudiw.rp.admin.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "eudiw-admin-web.security.enabled=false")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
class CspEnforceDevTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceUrlProvider resourceUrlProvider;

    @Test
    void versionedFaviconIsPublicOnDevSecurityChain() throws Exception {
        String faviconUrl = resourceUrlProvider.getForLookupPath("/favicon.ico");
        mockMvc.perform(get(faviconUrl)).andExpect(status().isOk());
        mockMvc.perform(get("/search")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should make the header menu available on the login page")
    void shouldMakeHeaderMenuAvailableOnLoginPage() throws Exception {
        String headerScriptUrl = resourceUrlProvider.getForLookupPath("/js/header.js");
        assertThat(headerScriptUrl).isNotNull().isNotEqualTo("/js/header.js");

        mockMvc.perform(get(headerScriptUrl)).andExpect(status().isOk());
        mockMvc.perform(get("/js/header.js")).andExpect(status().isOk());
        mockMvc.perform(get("/js/confirm_revocation.js")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/search")).andExpect(status().is3xxRedirection());
    }

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

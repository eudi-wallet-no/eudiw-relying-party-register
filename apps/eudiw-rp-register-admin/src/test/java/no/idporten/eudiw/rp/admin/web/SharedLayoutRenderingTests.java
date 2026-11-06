package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.thymeleaf.enabled=true")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
class SharedLayoutRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelyingPartiesService relyingPartiesService;

    @Test
    @DisplayName("Should show login and a link to the main content for unauthenticated users")
    void shouldShowLoginAndMainContentForUnauthenticatedUsers() throws Exception {
        String html = mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(html)
            .contains("href=\"#hovudinnhald\"", "<main id=\"hovudinnhald\" tabindex=\"-1\">",
                "id=\"header-nav-toggle\"", "id=\"header-navigation\"",
                "aria-controls=\"header-navigation\"", "aria-expanded=\"false\"",
                "href=\"/login\"", "Slack: #produkt-digital-lommebok",
                "Digitaliseringsdirektoratet</h2>", "Teknisk informasjon</h2>",
                "https://www.digdir.no/digdir/om-digitaliseringsdirektoratet/1547",
                "https://docs.digdir.no/docs/lommebok/lommebok_om.html",
                "mailto:servicedesk@digdir.no", "https://digdir-samarbeid.slack.com/archives/C08RD6BK4Q3")
            .doesNotContain("href=\"/logout\"", "Slack: #digital-lommebok");
    }

    @Test
    @WithMockUser(username = "administrator", roles = "ADMIN")
    @DisplayName("Should show administrator navigation and the administrator creation form")
    void shouldShowAdministratorNavigationAndCreationForm() throws Exception {
        String html = mockMvc.perform(get("/create"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(html)
            .contains("administrator", "href=\"/logout\"", "href=\"/search\"",
                "id=\"admin_create_form\"", "Slack: #digital-lommebok")
            .doesNotContain("Slack: #produkt-digital-lommebok");
    }

    @Test
    @DisplayName("Should show self-service navigation and the self-service creation form")
    void shouldShowSelfServiceNavigationAndCreationForm() throws Exception {
        String html = mockMvc.perform(get("/create").with(SecurityTestUtils.oidcLoginForOrgno("123456785")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(html)
            .contains("href=\"/logout\"", "href=\"/registrations\"",
                "id=\"create_form\"", "Slack: #produkt-digital-lommebok")
            .doesNotContain("id=\"admin_create_form\"", "Slack: #digital-lommebok");
    }
}

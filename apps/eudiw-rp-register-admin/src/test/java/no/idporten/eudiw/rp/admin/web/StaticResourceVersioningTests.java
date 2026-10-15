package no.idporten.eudiw.rp.admin.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.thymeleaf.enabled=true")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
class StaticResourceVersioningTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceUrlProvider resourceUrlProvider;

    @ParameterizedTest
    @ValueSource(strings = {
        "/css/index.css", "/css/digital-lommebok.css", "/css/styles.css", "/css/simple_tabs.css",
        "/js/simple_tabs.js", "/js/confirm_revocation.js", "/js/dynamic_edit_form_fields.js",
        "/inter/InterVariable.woff2", "/favicon.ico"
    })
    void servesContentHashedResourcesWithoutAuthentication(String path) throws Exception {
        byte[] bytes = new ClassPathResource("static" + path).getContentAsByteArray();
        String hash = DigestUtils.md5DigestAsHex(bytes);
        int extension = path.lastIndexOf('.');
        String expectedUrl = path.substring(0, extension) + "-" + hash + path.substring(extension);

        assertThat(resourceUrlProvider.getForLookupPath(path)).isEqualTo(expectedUrl);
        mockMvc.perform(get(expectedUrl))
            .andExpect(status().isOk())
            .andExpect(content().bytes(bytes));
        mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(content().bytes(bytes));
    }

    @Test
    void rendersVersionedStylesheetsAndFaviconWithContextPath() throws Exception {
        String html = mockMvc.perform(get("/register/login").contextPath("/register"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        for (String path : new String[]{"/inter/inter.css", "/css/index.css",
            "/css/digital-lommebok.css", "/css/styles.css", "/favicon.ico"}) {
            String versionedUrl = resourceUrlProvider.getForLookupPath(path);
            assertThat(versionedUrl).isNotNull().isNotEqualTo(path);
            assertThat(html).contains("href=\"/register" + versionedUrl + "\"");
        }
    }

    @Test
    void rewritesRelativeFontUrlsInsideVersionedStylesheet() throws Exception {
        String stylesheetUrl = resourceUrlProvider.getForLookupPath("/inter/inter.css");
        String fontUrl = resourceUrlProvider.getForLookupPath("/inter/InterVariable.woff2");

        String css = mockMvc.perform(get(stylesheetUrl))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(css).contains("url(\"" + fontUrl + "\")");
        assertThat(css).doesNotContain("url(\"InterVariable.woff2\")");
    }

    @Test
    void rejectsIncorrectContentHash() throws Exception {
        mockMvc.perform(get("/css/styles-00000000000000000000000000000000.css"))
            .andExpect(status().isNotFound());
    }
}

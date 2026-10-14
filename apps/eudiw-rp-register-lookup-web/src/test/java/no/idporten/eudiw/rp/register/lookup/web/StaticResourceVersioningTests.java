package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local-test")
@AutoConfigureMockMvc
class StaticResourceVersioningTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceUrlProvider resourceUrlProvider;

    @LocalServerPort
    private int port;

    @MockitoBean
    private RelyingPartiesService relyingPartiesService;

    @MockitoBean
    private CredentialsService credentialsService;

    @ParameterizedTest
    @ValueSource(strings = {
        "/css/index.css", "/css/digital-lommebok.css", "/css/styles.css", "/css/clickable_headers.css",
        "/js/header.js", "/js/searchForm.js", "/favicon.ico"
    })
    void servesContentHashedResources(String path) throws Exception {
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

    @ParameterizedTest
    @ValueSource(strings = {"", "/register"})
    void rendersVersionedLocalUrlsAndPreservesCdnUrl(String contextPath) throws Exception {
        String html = mockMvc.perform(get(contextPath + "/").contextPath(contextPath))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        for (String path : new String[]{"/css/index.css", "/css/digital-lommebok.css",
            "/css/styles.css", "/favicon.ico", "/js/header.js", "/js/searchForm.js"}) {
            String versionedUrl = resourceUrlProvider.getForLookupPath(path);
            assertThat(versionedUrl).isNotNull().isNotEqualTo(path);
            assertThat(html).contains("\"" + contextPath + versionedUrl + "\"");
        }
        assertThat(html).contains("href=\"https://altinncdn.no/fonts/inter/v4.1/inter.css\"");
    }

    @Test
    void versionsResourceUrlsOverHttpWithoutRemovingSecurityHeaders() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("href=\"" + resourceUrlProvider.getForLookupPath("/css/index.css") + "\"");
            assertThat(response.headers().firstValue("Content-Security-Policy")).isPresent();
        }
    }

    @Test
    void rejectsIncorrectContentHash() throws Exception {
        mockMvc.perform(get("/css/styles-00000000000000000000000000000000.css"))
            .andExpect(status().isNotFound());
    }
}

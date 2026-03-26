package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.config.Trustlist612Properties;
import no.idporten.eudiw.trustlist.config.TrustlistACAProperties;
import no.idporten.eudiw.trustlist.config.TrustlistPIDProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
@DisplayName("When accessing the index page")
class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trustlist612Properties list612Properties;

    @Autowired
    private TrustlistPIDProperties  trustlistPIDProperties;

    @Autowired
    private TrustlistACAProperties acaProperties;

    @Test
    @DisplayName("should return the expected HTML as string with trustlist titles and links")
    void index() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andReturn();

        String html = mvcResult.getResponse().getContentAsString();
        assertNotNull(html);

        // Verify that the HTML contains the expected trust list titles and links
        assertTrue(html.contains(list612Properties.schemeInformation().schemeName().langNo()));
        assertTrue(html.contains(list612Properties.trustlistPathXtsl()));
        assertTrue(html.contains(list612Properties.trustlistPath()));
        assertTrue(html.contains(list612Properties.trustlistPathSha2()));

        assertTrue(html.contains(acaProperties.schemeInformation().schemeName().langNo()));
        assertTrue(html.contains(acaProperties.path()));

        assertTrue(html.contains(trustlistPIDProperties.schemeInformation().schemeName().langNo()));
        assertTrue(html.contains(trustlistPIDProperties.path()));
    }

    @Test
    @DisplayName("properties for trust lists is loaded with non-empty content for trustlist names and paths")
    void propertiesForTrustlistsShouldBeLoadedWithNonEmptyContent() {

        assertNotNull(list612Properties);
        assertNotNull(list612Properties.schemeInformation());
        assertNotNull(list612Properties.schemeInformation().schemeName());
        assertNotNull(list612Properties.schemeInformation().schemeName().langNo());
        assertNotNull(list612Properties.trustlistPathXtsl());
        assertNotNull(list612Properties.trustlistPath());
        assertNotNull(list612Properties.trustlistPathSha2());

        assertNotNull(acaProperties);
        assertNotNull(acaProperties.schemeInformation());
        assertNotNull(acaProperties.schemeInformation().schemeName());
        assertNotNull(acaProperties.schemeInformation().schemeName().langNo());
        assertNotNull(acaProperties.path());

        assertNotNull(trustlistPIDProperties);
        assertNotNull(trustlistPIDProperties.schemeInformation());
        assertNotNull(trustlistPIDProperties.schemeInformation().schemeName());
        assertNotNull(trustlistPIDProperties.schemeInformation().schemeName().langNo());
        assertNotNull(trustlistPIDProperties.path());
    }
}
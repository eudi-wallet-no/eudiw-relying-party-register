package no.idporten.eudiw.trustlist.web;

import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.config.Trustlist612Properties;
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
    private Trustlist612Properties trustlist612Properties;

    @Autowired
    private Trustlist602Properties trustlist602Properties;


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
        assertTrue(html.contains(trustlist612Properties.schemeInformation().schemeName().langNo()));
        assertTrue(html.contains(trustlist612Properties.trustlistPathXtsl()));
        assertTrue(html.contains(trustlist612Properties.trustlistPath()));
        assertTrue(html.contains(trustlist612Properties.trustlistPathSha2()));

        assertTrue(html.contains(trustlist602Properties.getAcaTrustlist().schemeInformation().schemeName().langNo()));
        assertTrue(html.contains(trustlist602Properties.getAcaTrustlist().path()));

        assertTrue(html.contains(trustlist602Properties.getPidTrustlist().schemeInformation().schemeName().langNo()));
        assertTrue(html.contains(trustlist602Properties.getPidTrustlist().path()));
    }

    @Test
    @DisplayName("properties for trust lists is loaded with non-empty content for trustlist names and paths")
    void propertiesForTrustlistsShouldBeLoadedWithNonEmptyContent() {

        assertNotNull(trustlist612Properties);
        assertNotNull(trustlist612Properties.schemeInformation());
        assertNotNull(trustlist612Properties.schemeInformation().schemeName());
        assertNotNull(trustlist612Properties.schemeInformation().schemeName().langNo());
        assertNotNull(trustlist612Properties.trustlistPathXtsl());
        assertNotNull(trustlist612Properties.trustlistPath());
        assertNotNull(trustlist612Properties.trustlistPathSha2());

        assertNotNull(trustlist602Properties.tsl602());

        assertNotNull(trustlist602Properties.getAcaTrustlist());
        assertNotNull(trustlist602Properties.getAcaTrustlist().schemeInformation());
        assertNotNull(trustlist602Properties.getAcaTrustlist().schemeInformation().schemeName());
        assertNotNull(trustlist602Properties.getAcaTrustlist().schemeInformation().schemeName().langNo());
        assertNotNull(trustlist602Properties.getAcaTrustlist().path());

        assertNotNull(trustlist602Properties.getPidTrustlist());
        assertNotNull(trustlist602Properties.getPidTrustlist().schemeInformation());
        assertNotNull(trustlist602Properties.getPidTrustlist().schemeInformation().schemeName());
        assertNotNull(trustlist602Properties.getPidTrustlist().schemeInformation().schemeName().langNo());
        assertNotNull(trustlist602Properties.getPidTrustlist().path());
    }
}
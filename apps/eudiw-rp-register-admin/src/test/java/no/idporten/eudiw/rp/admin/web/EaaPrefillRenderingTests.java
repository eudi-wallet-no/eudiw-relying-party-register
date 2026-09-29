package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyEaaResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@TestPropertySource(properties = "spring.thymeleaf.enabled=true")
@DisplayName("When rendering the EAA prefill markup")
class EaaPrefillRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    private String renderEditFormWithEaa(String namespace, String intent) throws Exception {
        RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();
        UUID id = rpResource.id();
        RelyingPartyResource withEaa = rpResource.withRelyingPartyEaas(
            List.of(new RelyingPartyEaaResource(namespace, intent)));
        when(mockRpService.get(id)).thenReturn(withEaa);

        return mockMvc.perform(get("/edit/{id}", id))
                      .andExpect(status().isOk())
                      .andReturn()
                      .getResponse()
                      .getContentAsString();
    }

    @Test
    @DisplayName("then EAA values are emitted as data attributes, not inlined into a script")
    void testEaaValuesRenderedAsDataAttributes() throws Exception {
        String html = renderEditFormWithEaa("no:minid:mpid:1", "test");

        assertThat(html).contains("id=\"eaa_prefill\"");
        assertThat(html).contains("data-namespace=\"no:minid:mpid:1\"");
        assertThat(html).contains("data-intent=\"test\"");
        assertThat(html).doesNotContain("addEaa(");
    }

    @Test
    @DisplayName("then a double quote in the namespace is escaped and cannot break out of the attribute")
    void testDoubleQuoteInNamespaceIsEscaped() throws Exception {
        String html = renderEditFormWithEaa("\"onerror", "test");

        assertThat(html).contains("data-namespace=\"&quot;onerror\"");
        assertThat(html).doesNotContain("data-namespace=\"\"onerror\"");
        assertThat(html).doesNotContain("addEaa(");
    }
}
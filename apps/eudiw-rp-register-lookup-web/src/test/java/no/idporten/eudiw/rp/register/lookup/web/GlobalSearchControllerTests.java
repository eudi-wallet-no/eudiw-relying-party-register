package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.register.lookup.web.controllers.GlobalSearchController;
import no.idporten.eudiw.rp.register.lookup.web.resource.PagedResponse;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialsResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the global search controller")
@AutoConfigureMockMvc
public class GlobalSearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService lookupService;

    @SuppressWarnings("unused")
    @MockitoBean
    private CredentialsService mockCredentialsService;

    @Nested
    @DisplayName("when GET'ing to /search")
    class GlobalSearchGetTests {

        @Test
        public void testSearchRendersBothSectionsOnMatch() throws Exception {
            RelyingPartyResource rp = ResourceGenerator.generateRelyingPartyResource();
            when(lookupService.search(any()))
                .thenReturn(ResourceGenerator.generatePageResponse(List.of(rp)));
            CredentialResource credential = ResourceGenerator.generateCredentialResource()
                .withCredentialType("testbevis");
            when(mockCredentialsService.search(any()))
                .thenReturn(new CredentialsResource(List.of(credential)));

            mockMvc.perform(get("/search").param("searchTerm", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("global_search_view"))
                .andExpect(model().attribute(GlobalSearchController.searchTermAttrId, "test"))
                .andExpect(model().attribute(GlobalSearchController.relyingPartiesAttrId, List.of(rp)))
                .andExpect(model().attribute(GlobalSearchController.credentialsAttrId, List.of(credential)))
                .andExpect(model().attribute(GlobalSearchController.totalHitsAttrId, 2))
                .andExpect(content().string(containsString("Brukarstader")))
                .andExpect(content().string(containsString("Beviskatalog")));
        }

        @Test
        public void testSearchRendersEmptySectionsWhenServicesFail() throws Exception {
            when(lookupService.search(any()))
                .thenThrow(new RuntimeException("register down"));
            when(mockCredentialsService.search(any()))
                .thenThrow(new RuntimeException("credential registry down"));

            mockMvc.perform(get("/search").param("searchTerm", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("global_search_view"))
                .andExpect(model().attribute(GlobalSearchController.relyingPartiesAttrId, List.of()))
                .andExpect(model().attribute(GlobalSearchController.credentialsAttrId, List.of()))
                .andExpect(model().attribute(GlobalSearchController.totalHitsAttrId, 0))
                .andExpect(content().string(containsString("Fann ingen brukarstader som matcher søket.")))
                .andExpect(content().string(containsString("Fann ingen bevistypar som matcher søket.")));
        }

        @Test
        public void testSearchTrimsWhitespaceFromTerm() throws Exception {
            when(lookupService.search(any()))
                .thenReturn(ResourceGenerator.generatePageResponse(List.of()));
            when(mockCredentialsService.search(any()))
                .thenReturn(new CredentialsResource(List.of()));

            mockMvc.perform(get("/search").param("searchTerm", "  test  "))
                .andExpect(status().isOk())
                .andExpect(model().attribute(GlobalSearchController.searchTermAttrId, "test"));
        }

        @Test
        public void testSearchWithoutTermRendersEmptyPage() throws Exception {
            mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("global_search_view"))
                .andExpect(model().attribute(GlobalSearchController.searchTermAttrId, ""))
                .andExpect(model().attribute(GlobalSearchController.relyingPartiesAttrId, List.of()))
                .andExpect(model().attribute(GlobalSearchController.credentialsAttrId, List.of()))
                .andExpect(model().attribute(GlobalSearchController.totalHitsAttrId, 0))
                .andExpect(content().string(containsString("index-search__box")))
                .andExpect(content().string(containsString("Fann ingen brukarstader som matcher søket.")))
                .andExpect(content().string(containsString("Fann ingen bevistypar som matcher søket.")));

            verifyNoInteractions(lookupService, mockCredentialsService);
        }
    }
}
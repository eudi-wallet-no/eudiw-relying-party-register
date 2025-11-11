package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.SearchController;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the search controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class SearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("when POST'ing the /search endpoint")
    class SearchEndpointPostTests {
        @BeforeEach
        void setupMockRelyingPartiesService() {
            when(mockRpService.search(any()))
                .thenAnswer(invocationOnMock -> {
                    SearchRelyingPartyResource searchResource = invocationOnMock.getArgument(0, SearchRelyingPartyResource.class);

                    List<RelyingPartyResource> content =
                        List.of(ResourceGenerator.generateRelyingPartyResource()
                                                 .withName(searchResource.getSearchTerm()));
                    return ResourceGenerator.generatePageResponse(content);
                });
        }

        @Test
        public void testServiceCalledAndWithEmptyResource() throws Exception {
            mockMvc.perform(post("/search")
                    .formField("searchTerm", SearchForm.empty().searchTerm())
                    .formField("includeInactive", Boolean.toString(SearchForm.empty().includeInactive())))
                .andExpect(view().name("search_view"))
                .andExpect(model().attribute(SearchController.searchFormAttrId, SearchForm.empty()));

            SearchRelyingPartyResource expectedSearchResource =
                new SearchRelyingPartyResource(SearchForm.empty());
            verify(mockRpService).search(eq(expectedSearchResource));
        }

        @Test
        public void testServiceCalledAndWithCorrectSearchResource() throws Exception {
            SearchForm testSearchForm = ResourceGenerator.generateSearchForm();
            String includeInactiveStr = Boolean.toString(testSearchForm.includeInactive());

            mockMvc.perform(post("/search")
                                .formField("searchTerm", testSearchForm.searchTerm())
                                .formField("includeInactive", includeInactiveStr))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attribute(SearchController.searchFormAttrId, testSearchForm));
            SearchRelyingPartyResource expectedSearchResource =
                new SearchRelyingPartyResource(testSearchForm);
            verify(mockRpService).search(eq(expectedSearchResource));
        }

        @Test
        public void testInvalidSearchFormIsRejected() throws Exception {
            String invalidSearchTerm = "foobar$";
            mockMvc.perform(post("/search")
                                .formField("searchTerm", invalidSearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasFieldErrors(SearchController.searchFormAttrId, "searchTerm"));

            verify(mockRpService, times(0)).search(any());
        }
    }
}

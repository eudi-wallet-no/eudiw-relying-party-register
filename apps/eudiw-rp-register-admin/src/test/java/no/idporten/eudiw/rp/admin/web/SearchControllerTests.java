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
                    SearchRelyingPartyResource searchResource =
                        invocationOnMock.getArgument(0, SearchRelyingPartyResource.class);
                    return new RelyingPartiesResource(
                        List.of(ResourceGenerator.generateRelyingPartyResource()
                                                 .withName(searchResource.searchTerm())));
                });
        }
        @Test
        public void testBlankSearchBarGivesNoInteraction() throws Exception {
            String emptySearchTerm = "";
            mockMvc.perform(post("/search")
                                .formField("searchTerm", emptySearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasNoErrors(SearchController.searchFormAttrId));

            verifyNoInteractions(mockRpService);
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

            verify(mockRpService).search(eq(testSearchForm.toResource()));
        }

        @Test
        public void testInvalidSearchFormIsRejected() throws Exception {
            String invalidSearchTerm = "foobar$";
            mockMvc.perform(post("/search")
                                .formField("searchTerm", invalidSearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasFieldErrors(SearchController.searchFormAttrId, "searchTerm"));

            verifyNoInteractions(mockRpService);
        }
    }
}

package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.register.lookup.web.controllers.SearchController;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;


@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the search controller")
@AutoConfigureMockMvc
public class LookupControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService lookupService;

    @Nested
    @DisplayName("when POST'ing the / endpoint")
    class SearchEndpointPostTests {
        @BeforeEach
        void setupMockRelyingPartiesService() {
            when(lookupService.search(any()))
                .thenAnswer(invocationOnMock -> {
                    SearchRelyingPartyResource searchResource = invocationOnMock.getArgument(0, SearchRelyingPartyResource.class);

                    List<RelyingPartyResource> content =
                        List.of(ResourceGenerator.generateRelyingPartyResource()
                                                 .withName(searchResource.searchTerm()));
                    return ResourceGenerator.generatePageResponse(content);
                });
        }

        @Test
        public void testServiceCalledAndWithEmptyResource() throws Exception {
            mockMvc.perform(post("/")
                    .formField("searchTerm", SearchForm.empty().searchTerm()))
                .andExpect(view().name("search_view"))
                .andExpect(model().attribute(SearchController.searchFormAttrId, SearchForm.empty()));

            SearchRelyingPartyResource expectedSearchResource =
                new SearchRelyingPartyResource(SearchForm.empty());
            verify(lookupService).search(eq(expectedSearchResource));
        }

        @Test
        public void testServiceCalledAndWithCorrectSearchResource() throws Exception {
            SearchForm testSearchForm = ResourceGenerator.generateSearchForm();

            mockMvc.perform(post("/")
                                .formField("searchTerm", testSearchForm.searchTerm()))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attribute(SearchController.searchFormAttrId, testSearchForm));
            SearchRelyingPartyResource expectedSearchResource =
                new SearchRelyingPartyResource(testSearchForm);
            verify(lookupService).search(eq(expectedSearchResource));
        }

        @Test
        public void testInvalidSearchFormIsRejected() throws Exception {
            String invalidSearchTerm = "foobar$";
            mockMvc.perform(post("/")
                                .formField("searchTerm", invalidSearchTerm))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasFieldErrors(SearchController.searchFormAttrId, "searchTerm"));

            verify(lookupService, times(0)).search(any());
        }
    }
}

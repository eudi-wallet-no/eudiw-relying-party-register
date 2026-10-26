package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.SearchController;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.PagedResponse;
import no.idporten.eudiw.rp.admin.web.resource.RegistrationSearchResult;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the search controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class SearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    @WithMockUser(roles = "USER")
    @DisplayName("Should prevent non-administrators from searching registrations")
    void shouldPreventNonAdministratorsFromSearchingRegistrations(String method) throws Exception {
        mockMvc.perform(method.equals("GET") ? get("/search") : post("/search").with(csrf()))
            .andExpectAll(status().isNotFound(), view().name("error/404"));
        verifyNoInteractions(mockRpService);
    }

    @Nested
    @DisplayName("when POST'ing the /search endpoint")
    class SearchEndpointPostTests {
        @BeforeEach
        void setupMockRelyingPartiesService() {
            when(mockRpService.search(any()))
                .thenAnswer(invocationOnMock -> {
                    SearchRelyingPartyResource searchResource = invocationOnMock.getArgument(0, SearchRelyingPartyResource.class);

                    var relyingPartyResource = ResourceGenerator.generateRelyingPartyResource();
                    var result = new RegistrationSearchResult(
                        relyingPartyResource.id(), relyingPartyResource.orgno(), relyingPartyResource.orgName(),
                        searchResource.getSearchTerm(), relyingPartyResource.relyingPartyEntitlements(),
                        relyingPartyResource.createdMs(), relyingPartyResource.lastUpdatedMs(),
                        relyingPartyResource.accessCertificates().size(), relyingPartyResource.issuerCertificates().size());
                    return new PagedResponse<>(List.of(result), new PagedResponse.PageMetadata(25, 0, 1, 1));
                });
        }

        @Test
        public void testServiceCalledAndWithEmptyResource() throws Exception {
            SearchForm searchForm = SearchForm.empty();
            mockMvc.perform(post("/search")
                    .with(csrf())
                    .formField("searchTerm", searchForm.getSearchTerm())
                    .formField("includeInactive", Boolean.toString(searchForm.isIncludeInactive())))
                .andExpect(view().name("search_view"))
                .andExpect(model().attribute(SearchController.SEARCH_FORM_ATTR, searchForm));

            SearchRelyingPartyResource expectedSearchResource =
                searchForm.toResource();
            verify(mockRpService).search(eq(expectedSearchResource));
        }

        @Test
        public void testServiceCalledAndWithCorrectSearchResource() throws Exception {
            SearchForm testSearchForm = ResourceGenerator.generateSearchForm();
            String includeInactiveStr = Boolean.toString(testSearchForm.isIncludeInactive());

            mockMvc.perform(post("/search")
                                .with(csrf())
                                .formField("searchTerm", testSearchForm.getSearchTerm())
                                .formField("includeInactive", includeInactiveStr))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attribute(SearchController.SEARCH_FORM_ATTR, testSearchForm));
            SearchRelyingPartyResource expectedSearchResource =
                testSearchForm.toResource();
            verify(mockRpService).search(eq(expectedSearchResource));
        }

        @Test
        public void testInvalidSearchFormIsRejected() throws Exception {
            String invalidSearchTerm = "foobar$";
            mockMvc.perform(post("/search")
                                .with(csrf())
                                .formField("searchTerm", invalidSearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasFieldErrors(SearchController.SEARCH_FORM_ATTR, "searchTerm"));

            verify(mockRpService, times(0)).search(any());
        }
    }
}

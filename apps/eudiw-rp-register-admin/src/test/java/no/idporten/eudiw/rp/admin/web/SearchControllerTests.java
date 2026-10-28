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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should prevent non-administrators from searching registrations")
    void shouldPreventNonAdministratorsFromSearchingRegistrations() throws Exception {
        mockMvc.perform(get("/search"))
            .andExpectAll(status().isNotFound(), view().name("error/404"));
        verifyNoInteractions(mockRpService);
    }

    @Nested
    @DisplayName("when GET'ing the /search endpoint")
    class SearchEndpointGetTests {
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
            mockMvc.perform(get("/search")
                    .param("searchTerm", searchForm.getSearchTerm())
                    .param("includeInactive", Boolean.toString(searchForm.isIncludeInactive())))
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

            mockMvc.perform(get("/search")
                                            .param("searchTerm", testSearchForm.getSearchTerm())
                                .param("includeInactive", includeInactiveStr))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attribute(SearchController.SEARCH_FORM_ATTR, testSearchForm));
            SearchRelyingPartyResource expectedSearchResource =
                testSearchForm.toResource();
            verify(mockRpService).search(eq(expectedSearchResource));
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Should preserve the inactive filter without a hidden checkbox marker")
        void shouldPreserveTheInactiveFilterWithoutAHiddenCheckboxMarker(boolean includeInactive) throws Exception {
            var request = get("/search").param("searchTerm", "Thomas");
            if (includeInactive) {
                request.param("includeInactive", "true");
            }

            String html = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            assertThat(html).doesNotContain("name=\"_includeInactive\"");
            var checkbox = Pattern.compile("<input\\b[^>]*id=\"includeInactive\"[^>]*>").matcher(html);
            assertThat(checkbox.find()).isTrue();
            assertThat(checkbox.group()).contains("name=\"includeInactive\"", "value=\"true\"");
            assertThat(checkbox.group().contains("checked")).isEqualTo(includeInactive);

            verify(mockRpService).search(argThat(resource -> resource.isIncludeInactive() == includeInactive));
        }

        @Test
        public void testInvalidSearchFormIsRejected() throws Exception {
            String invalidSearchTerm = "foobar$";
            mockMvc.perform(get("/search")
                                            .param("searchTerm", invalidSearchTerm)
                                .param("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasFieldErrors(SearchController.SEARCH_FORM_ATTR, "searchTerm"));

            verify(mockRpService, times(0)).search(any());
        }
    }
}

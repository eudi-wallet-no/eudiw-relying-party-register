package no.idporten.eudiw.rp.admin.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import no.idporten.eudiw.rp.admin.service.accesscertificates.X509CertificateConverter;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.testdata.CertificatesGenerator;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.SearchRelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.CsrForm;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartiesView;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the relying party certificates service")
@AutoConfigureMockMvc
public class SearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("when GET'ing the /search endpoint")
    class SearchEndpointGetTests {
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
        public void testServiceCalledAndWithCorrectSearchResource() throws Exception {
            SearchForm testSearchForm = ResourceGenerator.generateSearchForm();
            String includeInactiveStr = Boolean.valueOf(testSearchForm.includeInactive()).toString();

            mockMvc.perform(get("/search")
                                .formField("searchTerm", testSearchForm.searchTerm())
                                .formField("includeInactive", includeInactiveStr))
                .andExpect(view().name("search_view"))
                .andExpect(model().attributeExists(SearchController.fullResultsAttrId))
                .andExpect(model().attribute(SearchController.searchFormAttrId, testSearchForm));

            verify(mockRpService).search(eq(testSearchForm.toResource()));
        }

        @Test
        public void testInvalidSearchFormIsRejected() throws Exception {
            String invalidSearchTerm = "foobar$";
            mockMvc.perform(get("/search")
                                .formField("searchTerm", invalidSearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasFieldErrors(SearchController.searchFormAttrId, "searchTerm"))
                   .andExpect(model().attributeDoesNotExist(SearchController.fullResultsAttrId));

            verifyNoInteractions(mockRpService);
        }

        @Test
        public void testBlankSearchBarGivesNoInteraction() throws Exception {
            String emptySearchTerm = "";
            mockMvc.perform(get("/search")
                                .formField("searchTerm", emptySearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasNoErrors(SearchController.searchFormAttrId))
                   .andExpect(model().attributeDoesNotExist(SearchController.fullResultsAttrId));

            verifyNoInteractions(mockRpService);
        }
    }

    @Nested
    @DisplayName("when GET'ing the /details endpoint for a given RP ID ...")
    class DetailsEndpointGetTests {
        @BeforeEach
        void setupMockRelyingPartiesService() throws Exception {
            RelyingPartyAccessCertificatesResource dummyCertsResource =
                new RelyingPartyAccessCertificatesResource(List.of(
                    ResourceGenerator.generateCertificateResource()));
            when(mockRpService.getCertificatesForRelyingParty(any()))
                .thenReturn(dummyCertsResource);
        }

        @Test
        @DisplayName("then RP fetched from search results if exists, and correct view/model is used")
        public void testControllerFetchesRpFromSearchResultsIfIdExists() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();

            RelyingPartiesView dummyPreExistingSearchResults =
                RelyingPartiesView.fromResource(
                    new RelyingPartiesResource(List.of(rpResource)));

            mockMvc.perform(get("/details")
                                .queryParam("id", id.toString())
                                .sessionAttr(SearchController.fullResultsAttrId, dummyPreExistingSearchResults))
                .andExpect(status().isOk())
                .andExpect(view().name("details_view"))
                .andExpect(model().attribute(SearchController.detailedViewDataAttrId, rpResource));

            verify(mockRpService, times(0)).get(eq(id));
            verify(mockRpService).getCertificatesForRelyingParty(eq(id));
        }

        @Test
        @DisplayName("then RP fetched via service if ID not in search results, and correct view/model is used")
        public void testControllerQueriesServiceIfIdNotInSearchResults()
            throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(any())).thenReturn(rpResource);

            RelyingPartiesView dummyPreExistingSearchResults =
                RelyingPartiesView.fromResource(
                    ResourceGenerator.generateRelyingPartiesResource());

            mockMvc.perform(get("/details")
                                .queryParam("id", id.toString())
                                .sessionAttr(SearchController.fullResultsAttrId, dummyPreExistingSearchResults))
                   .andExpect(status().isOk())
                   .andExpect(view().name("details_view"))
                   .andExpect(model().attribute(SearchController.detailedViewDataAttrId, rpResource));

            verify(mockRpService).get(eq(id));
            verify(mockRpService).getCertificatesForRelyingParty(eq(id));
        }
    }

    @Nested
    @DisplayName("when GET'ing the /registerCsr endpoint for a given RP ID ...")
    class RegisterCsrEndpointGetTests {

        @Test
        @DisplayName("then RP fetched from search results if exists, and correct view/model is used")
        void testControllerGetsRpFromSearchResultsIfExists() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();

            RelyingPartiesView dummyPreExistingSearchResults =
                RelyingPartiesView.fromResource(
                    new RelyingPartiesResource(List.of(rpResource)));

            mockMvc.perform(get("/registerCsr")
                                .queryParam("id", id.toString())
                                .sessionAttr(SearchController.fullResultsAttrId, dummyPreExistingSearchResults))
                   .andExpect(status().isOk())
                   .andExpect(view().name("csr_form_view"))
                   .andExpect(model().attribute(SearchController.detailedViewDataAttrId, rpResource))
                   .andExpect(model().attribute(SearchController.csrFormAttrId, CsrForm.empty()));

            verify(mockRpService, times(0)).get(eq(id));
        }
    }

    @Nested
    @DisplayName("when POST'ing a CSR to the /registerCsr endpoint for a given RP ID ...")
    class RegisterCsrEndpointPostTests {

        @BeforeEach
        void setupMockRelyingPartiesService() throws Exception {
        }

        @Test
        @DisplayName("then form is accepted if the CSR is well-formed and ID exists")
        void testCsrFormAcceptedIfCsrWellFormedAndIdExists() throws Exception {
            // set up service with a dummy certificate response
            RelyingPartyAccessCertificateResource dummyCertResource =
                ResourceGenerator.generateCertificateResource();
            when(mockRpService.requestCertificateForRelyingParty(any(), any()))
                .thenReturn(dummyCertResource);

            // setup dummy search results session attribute
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            RelyingPartiesView dummyPreExistingSearchResults =
                RelyingPartiesView.fromResource(
                    new RelyingPartiesResource(List.of(rpResource)));

            PKCS10CertificationRequest csr = CertificatesGenerator.generatePKCS10Csr();
            String csrPemStr = PKCS10CertificationRequestConverter.toString(csr);

            mockMvc.perform(post("/registerCsr")
                                .queryParam("id", id.toString())
                                .formField("csr", csrPemStr)
                                .sessionAttr(SearchController.fullResultsAttrId, dummyPreExistingSearchResults)
                   )
                   .andExpect(status().isOk())
                   .andExpect(view().name("csr_submit_success_view"))
                   .andExpect(model().attribute(SearchController.newCertificateAttrId, dummyCertResource.toSummary()));
        }

        @Test
        @DisplayName("then form is rejected if CSR is not well-formed, and view returns to CSR form")
        void testFormRejectedIfCsrInvalid() throws Exception {
            // setup dummy search results session attribute
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            RelyingPartiesView dummyPreExistingSearchResults =
                RelyingPartiesView.fromResource(
                    new RelyingPartiesResource(List.of(rpResource)));

            PKCS10CertificationRequest csr = CertificatesGenerator.generatePKCS10Csr();
            String validCsrPemStr = PKCS10CertificationRequestConverter.toString(csr);
            String invalidCsrPemStr = validCsrPemStr.replace('\n', 'x');

            mockMvc.perform(post("/registerCsr")
                                .queryParam("id", id.toString())
                                .formField("csr", invalidCsrPemStr)
                                .sessionAttr(SearchController.fullResultsAttrId, dummyPreExistingSearchResults)
                   )
                   .andExpect(status().isOk())
                   .andExpect(view().name("csr_form_view"))
                   .andExpect(model().attributeHasFieldErrors(SearchController.csrFormAttrId, "csr"))
            ;

        }
    }

}

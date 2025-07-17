package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import no.idporten.eudiw.rp.admin.testdata.CertificatesGenerator;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyCreateForm;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyEditForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.CsrForm;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificatesResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.util.Collections;
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
            String includeInactiveStr = Boolean.toString(testSearchForm.includeInactive());

            mockMvc.perform(post("/search")
                                .formField("searchTerm", testSearchForm.searchTerm())
                                .formField("includeInactive", includeInactiveStr))
                .andExpect(view().name("search_view"))
                .andExpect(model().attribute(AdminController.searchFormAttrId, testSearchForm));

            verify(mockRpService).search(eq(testSearchForm.toResource()));
        }

        @Test
        public void testInvalidSearchFormIsRejected() throws Exception {
            String invalidSearchTerm = "foobar$";
            mockMvc.perform(post("/search")
                                .formField("searchTerm", invalidSearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasFieldErrors(AdminController.searchFormAttrId, "searchTerm"));

            verifyNoInteractions(mockRpService);
        }

        @Test
        public void testBlankSearchBarGivesNoInteraction() throws Exception {
            String emptySearchTerm = "";
            mockMvc.perform(get("/search")
                                .formField("searchTerm", emptySearchTerm)
                                .formField("includeInactive", "true"))
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attributeHasNoErrors(AdminController.searchFormAttrId));

            verifyNoInteractions(mockRpService);
        }
    }

    @Nested
    @DisplayName("when GET'ing the /details endpoint for a given RP ID ...")
    class DetailsEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the correct RP and certificates is loaded")
        public void testCorrectViewAndModelAttributes()
            throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            RelyingPartyAccessCertificatesResource certsResource =
                new RelyingPartyAccessCertificatesResource(List.of(
                    ResourceGenerator.generateCertificateResource()));
            when(mockRpService.getCertificatesForRelyingParty(id))
                .thenReturn(certsResource);

            mockMvc.perform(get("/details")
                                .queryParam("id", id.toString()))
                   .andExpect(status().isOk())
                   .andExpect(view().name("details_view"))
                   .andExpect(model().attribute(AdminController.detailedViewDataAttrId, rpResource))
                   .andExpect(model().attribute(AdminController.certificateSummariesAttrId, certsResource.toSummaries()));

            verify(mockRpService).get(eq(id));
            verify(mockRpService).getCertificatesForRelyingParty(eq(id));
        }
    }

    @Nested
    @DisplayName("when GET'ing the /registerCsr endpoint for a given RP ID ...")
    class RegisterCsrEndpointGetTests {

        @Test
        @DisplayName("then the correct view with the expected RP attribute is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            mockMvc.perform(get("/registerCsr")
                                .queryParam("id", id.toString()))
                   .andExpect(status().isOk())
                   .andExpect(view().name("csr_form_view"))
                   .andExpect(model().attribute(AdminController.detailedViewDataAttrId, rpResource))
                   .andExpect(model().attribute(AdminController.csrFormAttrId, CsrForm.empty()));

            verify(mockRpService, times(1)).get(eq(id));
        }
    }

    @Nested
    @DisplayName("when POST'ing a CSR to the /registerCsr endpoint for a given RP ID ...")
    class RegisterCsrEndpointPostTests {

        @Test
        @DisplayName("then form accepted if CSR well-formed, and correct services called, view, and model")
        void testCsrFormAcceptedIfCsrWellFormedAndIdExists() throws Exception {

            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            // set up service with a dummy certificate response
            RelyingPartyAccessCertificateResource dummyCertResource =
                ResourceGenerator.generateCertificateResource();

            PKCS10CertificationRequest csr = CertificatesGenerator.generatePKCS10Csr();
            RelyingPartyCsrResource csrResource = new RelyingPartyCsrResource(csr);
            when(mockRpService.requestCertificateForRelyingParty(id, csrResource))
                .thenReturn(dummyCertResource);

            String csrPemStr = PKCS10CertificationRequestConverter.toString(csr);

            mockMvc.perform(post("/registerCsr")
                                .queryParam("id", id.toString())
                                .formField("csr", csrPemStr))
                   .andExpect(status().isOk())
                   .andExpect(view().name("csr_submit_success_view"))
                   .andExpect(model().attribute(AdminController.newCertificateAttrId, dummyCertResource.toSummary()));

            verify(mockRpService).get(eq(id));
            verify(mockRpService).requestCertificateForRelyingParty(eq(id), eq(csrResource));
        }

        @Test
        @DisplayName("then form is rejected if CSR is not well-formed, and view returns to CSR form")
        void testFormRejectedIfCsrInvalid() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            PKCS10CertificationRequest csr = CertificatesGenerator.generatePKCS10Csr();
            String validCsrPemStr = PKCS10CertificationRequestConverter.toString(csr);
            String invalidCsrPemStr = validCsrPemStr.replace('\n', 'x');

            mockMvc.perform(post("/registerCsr")
                                .queryParam("id", id.toString())
                                .formField("csr", invalidCsrPemStr))
                   .andExpect(status().isOk())
                   .andExpect(view().name("csr_form_view"))
                   .andExpect(model().attributeHasFieldErrors(AdminController.csrFormAttrId, "csr"));
        }
    }

    @Nested
    @DisplayName("When GET'ing the /details/edit endpoint for a given RP ID ...")
    class EditEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the expected edit form and RP is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            RelyingPartyEditForm expectedEditForm =
                RelyingPartyEditForm.prefillFromRelyingPartyResource(rpResource);

            mockMvc.perform(get("/details/edit")
                                .queryParam("id", id.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_form_view"))
                .andExpect(model().attribute(AdminController.editFormAttrId, expectedEditForm))
                .andExpect(model().attribute(AdminController.detailedViewDataAttrId, rpResource));

            verify(mockRpService).get(eq(id));
        }
    }

    @Nested
    @DisplayName("When POST'ing edit forms to the /details/edit endpoint for a given RP ID ...")
    class EditEndpointPostTests {
        @Test
        @DisplayName("then form accepted if well-formed, and correct services called, view, and model")
        void testEditFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyEntitlement entitlement = RelyingPartyEntitlement.SERVICE_PROVIDER;
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource()
                                 .withRelyingPartyEntitlements(List.of(entitlement.toResource()))
                                 .withRelyingPartyEaas(List.of());
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            RelyingPartyEditForm editForm =
                RelyingPartyEditForm.prefillFromRelyingPartyResource(rpResource);

            mockMvc.perform(post("/details/edit")
                                .queryParam("id", id.toString())
                                .formField("name", editForm.getName())
                                .formField("publicSector", Boolean.toString(editForm.isPublicSector()))
                                .formField("active", Boolean.toString(editForm.isActive()))
                                .formField("entitlements", entitlement.getUri()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/details?id=%s".formatted(id.toString())));

            EditRelyingPartyResource expectedEditResource = editForm.toResource();
            verify(mockRpService).edit(id, expectedEditResource);
        }

        @Test
        @DisplayName("then form rejected on invalid fields, and view returns to the edit form")
        void testEditFormRejectedOnFieldInvalidation() throws Exception {
            RelyingPartyEntitlement entitlement = RelyingPartyEntitlement.SERVICE_PROVIDER;
            String invalidName = "fooBar$";
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource()
                    .withName(invalidName)
                    .withRelyingPartyEntitlements(List.of(entitlement.toResource())) // empty entitlements and EAAs for simplicity
                    .withRelyingPartyEaas(List.of());
            UUID id = UUID.randomUUID();
            when(mockRpService.get(id)).thenReturn(rpResource);

            RelyingPartyEditForm editForm =
                RelyingPartyEditForm.prefillFromRelyingPartyResource(rpResource);

            mockMvc.perform(post("/details/edit")
                                .queryParam("id", id.toString())
                                .formField("name", invalidName)
                                .formField("publicSector", Boolean.toString(rpResource.publicSector()))
                                .formField("active", Boolean.toString(rpResource.active()))
                                .formField("entitlements", entitlement.getUri()))
                   // assert edit form invalid (should only have error in the name field)
                   .andExpect(model().attributeHasFieldErrors(AdminController.editFormAttrId, "name"))
                   .andExpect(model().attributeErrorCount(AdminController.editFormAttrId, 1))

                   // assert that view returns to edit form, for the given RP and
                   // with the unsubmitted form data.
                   .andExpect(status().isOk())
                   .andExpect(view().name("edit_form_view"))
                   .andExpect(model().attribute(AdminController.detailedViewDataAttrId, rpResource))
                   .andExpect(model().attribute(AdminController.editFormAttrId, editForm));

            verify(mockRpService).get(id);
        }
    }

    @Nested
    @DisplayName("When GET'ing the create endpoint")
    class CreateEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the expected create form is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyCreateForm createForm = new RelyingPartyCreateForm();

            mockMvc.perform(get("/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("create_form_view"))
                .andExpect(model().attribute(AdminController.createFormAttrId, createForm));
        }
    }

    @Nested
    @DisplayName("When POST'ing create forms to the /create endpoint")
    class CreateEndpointPostTests {
        @Test
        @DisplayName("then form accepted if well-formed, and correct services called, view, and model")
        void testCreateFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyCreateForm createForm = new RelyingPartyCreateForm();
            createForm.setName(ResourceGenerator.generateName());
            createForm.setPublicSector(ResourceGenerator.generateBoolean());
            createForm.setOrgno(TestDataGenerator.generateValidOrgno());
            RelyingPartyEntitlement entitlement = RelyingPartyEntitlement.SERVICE_PROVIDER;
            createForm.setEntitlements(List.of(entitlement.toFormField()));

            CreateRelyingPartyResource expectedCreateResource = createForm.toResource();
            RelyingPartyResource rpResource = new RelyingPartyResource(
                UUID.randomUUID(),
                createForm.getOrgno(),
                createForm.getName(),
                createForm.isPublicSector(),
                List.of(entitlement.toResource()),
                Collections.emptyList(),
                1,
                1,
                true
            );

            when(mockRpService.create(expectedCreateResource)).thenReturn(rpResource);
            mockMvc.perform(post("/create")
                    .formField("orgno", createForm.getOrgno())
                    .formField("name", createForm.getName())
                    .formField("publicSector", Boolean.toString(createForm.isPublicSector()))
                    .formField("entitlements", entitlement.getUri()))
                .andExpect(status().is3xxRedirection());
            ;

            verify(mockRpService).create(expectedCreateResource);
        }
    }
}

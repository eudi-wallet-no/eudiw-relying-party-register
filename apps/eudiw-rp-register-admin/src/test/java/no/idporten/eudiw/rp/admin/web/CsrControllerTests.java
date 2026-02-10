package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.testdata.CertificatesGenerator;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.CsrController;
import no.idporten.eudiw.rp.admin.web.controllers.SearchController;
import no.idporten.eudiw.rp.admin.web.form.CsrAccessForm;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummaryBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the CSR registration controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class CsrControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    RelyingPartyCertificateSummaryBuilder relyingPartyCertificateSummaryBuilder;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("when GET'ing the CSR endpoint for a given RP ID ...")
    class RegisterCsrEndpointGetTests {

        @Test
        @DisplayName("then the correct view with the expected RP attribute is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            mockMvc.perform(get("/csr/access/" + id).with(csrf()))
                   .andExpectAll(
                       status().isOk(),
                       view().name("access_csr_form_view"),
                       model().attribute(SearchController.DETAILED_VIEW_DATA_ATTR, rpResource),
                       model().attribute(CsrController.CSR_FORM_ATTR, CsrAccessForm.empty()));

            verify(mockRpService, times(1)).get(eq(id));
        }
    }

    @Nested
    @DisplayName("when POST'ing a CSR to the CSR endpoint for a given RP ID ...")
    class RegisterCsrEndpointPostTests {

        @Test
        @DisplayName("then form accepted if CSR well-formed, and correct services called, view, and model")
        void testCsrFormAcceptedIfCsrWellFormedAndIdExists() throws Exception {

            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            // set up service with a dummy certificate response
            RelyingPartyCertificateResource dummyCertResource =
                ResourceGenerator.generateCertificateResource();

            PKCS10CertificationRequest csr = CertificatesGenerator.generatePKCS10Csr();
            RelyingPartyCsrResource csrResource = new RelyingPartyCsrResource(csr);
            when(mockRpService.requestCertificateForRelyingParty(id, csrResource))
                .thenReturn(dummyCertResource);

            String csrPemStr = PKCS10CertificationRequestConverter.toString(csr);

            mockMvc.perform(post("/csr/access/" + id)
                                .with(csrf())
                                .formField("csrField", csrPemStr))
                   .andExpectAll(
                       status().isOk(),
                       view().name("csr_submit_success_view"),
                       model().attribute(CsrController.NEW_CERTIFICATE_ATTR, relyingPartyCertificateSummaryBuilder.build(dummyCertResource)));

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

            mockMvc.perform(post("/csr/access/" + id)
                                .with(csrf())
                                .formField("csrField", invalidCsrPemStr))
                   .andExpectAll(
                       status().isOk(),
                       view().name("access_csr_form_view"),
                       model().hasErrors(),
                       model().attributeHasFieldErrors(CsrController.CSR_FORM_ATTR, "csrField"));
        }

        @Test
        @DisplayName("then the correct view and model is used if there was an error posting valid CSR")
        public void testCorrectHandlingOfErrorResponse() throws Exception {

            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();

            when(mockRpService.get(any())).thenReturn(rpResource);
            when(mockRpService.requestCertificateForRelyingParty(any(), any()))
                .thenThrow(ErrorResponseException.class);

            String csrStr =
                PKCS10CertificationRequestConverter.toString(
                    ResourceGenerator.generateCsrResource().csr());

            mockMvc.perform(post("/csr/access/" + rpResource.id())
                                .with(csrf())
                                .formField("csrField", csrStr))
                .andExpectAll(
                    status().isOk(),
                    view().name("access_csr_form_view"),
                    model().attributeExists(CsrController.ERROR_RESPONSE_MSG_ATTR));
        }
    }
}

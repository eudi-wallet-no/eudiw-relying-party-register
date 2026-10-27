package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.accesscertificates.PKCS10CertificationRequestConverter;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.testdata.CertificatesGenerator;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.CsrController;
import no.idporten.eudiw.rp.admin.web.controllers.SearchController;
import no.idporten.eudiw.rp.admin.web.form.CsrAccessForm;
import no.idporten.eudiw.rp.admin.web.form.CsrIssuerForm;
import no.idporten.eudiw.rp.admin.web.resource.certificates.IssuerCsrResource;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;
import no.idporten.eudiw.rp.admin.web.search.resultsview.RelyingPartyCertificateSummaryBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Should show the access CSR form for the owning organisation and administrators")
        void shouldShowAccessCsrFormForAuthorizedUsers(boolean administrator) throws Exception {
            RelyingPartyResource rpResource =
                ResourceGenerator.generateRelyingPartyResource();
            UUID id = rpResource.id();
            when(mockRpService.get(id)).thenReturn(rpResource);

            mockMvc.perform(get("/csr/access/" + id).with(login(rpResource, administrator)))
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

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Should issue an access certificate for the owning organisation and administrators")
        void shouldIssueAccessCertificateForAuthorizedUsers(boolean administrator) throws Exception {

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
                                .with(login(rpResource, administrator))
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
                       model().attributeHasFieldErrors(CsrController.CSR_FORM_ATTR, "csrField"),
                       model().attributeDoesNotExist(CsrController.NEW_CERTIFICATE_ATTR));

            verifyNoCertificateRequests();
        }

        @ParameterizedTest
        @CsvSource({"access, false", "issuer, false", "access, true", "issuer, true"})
        @DisplayName("Should show issuance rejection or timeout without certificate success")
        void shouldShowIssuanceFailureWithoutSuccess(String type, boolean timeout) throws Exception {
            RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
            when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);
            RuntimeException failure = timeout
                ? new AdminServiceException("Certificate issuance outcome is unknown", new SocketTimeoutException("Read timed out"))
                : new ErrorResponseException("ca_error", "CA rejected issuance");
            if (type.equals("access")) {
                when(mockRpService.requestCertificateForRelyingParty(eq(relyingParty.id()), any())).thenThrow(failure);
            } else {
                when(mockRpService.requestIssuerCertificateForEntitlement(eq(relyingParty.id()), any())).thenThrow(failure);
            }

            var result = mockMvc.perform(post("/csr/" + type + "/" + relyingParty.id())
                    .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno()))
                    .with(csrf())
                    .formField("csrField", validCsr())
                    .formField("entitlement", "pid_provider"))
                .andExpectAll(
                    status().is(timeout ? 500 : 200),
                    view().name(timeout ? "error/5xx" : type + "_csr_form_view"),
                    model().attributeDoesNotExist(CsrController.NEW_CERTIFICATE_ATTR),
                    header().doesNotExist("Location"));
            if (timeout) {
                result.andExpect(content().string(containsString("Det har skjedd ein uventa feil")));
            } else {
                result.andExpectAll(
                    model().attribute(CsrController.ERROR_RESPONSE_MSG_ATTR, "exception.error_response"),
                    content().string(containsString("Førespurnaden din ble avvist")));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Should show the issuer CSR form for the owning organisation and administrators")
    void shouldShowIssuerCsrFormForAuthorizedUsers(boolean administrator) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);

        mockMvc.perform(get("/csr/issuer/" + relyingParty.id()).with(login(relyingParty, administrator)))
            .andExpectAll(
                status().isOk(),
                view().name("issuer_csr_form_view"),
                model().attribute(CsrController.DETAILED_VIEW_DATA_ATTR, relyingParty),
                model().attribute(CsrController.CSR_FORM_ATTR, CsrIssuerForm.empty()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Should issue an issuer certificate for the owning organisation and administrators")
    void shouldIssueIssuerCertificateForAuthorizedUsers(boolean administrator) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);
        PKCS10CertificationRequest csr = CertificatesGenerator.generatePKCS10Csr();
        IssuerCsrResource request = new IssuerCsrResource(csr, "pid_provider");
        RelyingPartyCertificateResource generated = ResourceGenerator.generateCertificateResource();
        RelyingPartyCertificateResource certificate = new RelyingPartyCertificateResource(
            generated.certificate(), generated.id(), request.entitlement(), -1);
        when(mockRpService.requestIssuerCertificateForEntitlement(relyingParty.id(), request)).thenReturn(certificate);

        mockMvc.perform(post("/csr/issuer/" + relyingParty.id())
                .with(login(relyingParty, administrator))
                .with(csrf())
                .formField("csrField", PKCS10CertificationRequestConverter.toString(csr))
                .formField("entitlement", request.entitlement()))
            .andExpectAll(
                status().isOk(),
                view().name("csr_issuer_submit_success_view"),
                model().attribute(CsrController.NEW_CERTIFICATE_ATTR, relyingPartyCertificateSummaryBuilder.build(certificate)),
                model().attributeDoesNotExist(CsrController.ERROR_RESPONSE_MSG_ATTR));

        verify(mockRpService).requestIssuerCertificateForEntitlement(relyingParty.id(), request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not a CSR"})
    @DisplayName("Should reject missing or malformed issuer CSR before requesting a certificate")
    void shouldRejectInvalidIssuerCsr(String csr) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);

        mockMvc.perform(post("/csr/issuer/" + relyingParty.id())
                .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno()))
                .with(csrf())
                .formField("csrField", csr)
                .formField("entitlement", "pid_provider"))
            .andExpectAll(
                status().isOk(),
                view().name("issuer_csr_form_view"),
                model().attributeHasErrors(CsrController.CSR_FORM_ATTR),
                model().attributeDoesNotExist(CsrController.NEW_CERTIFICATE_ATTR));

        verifyNoCertificateRequests();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Should reject missing or blank issuer entitlement before requesting a certificate")
    void shouldRejectMissingIssuerEntitlement(String entitlement) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);
        var request = post("/csr/issuer/" + relyingParty.id())
            .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno()))
            .with(csrf())
            .formField("csrField", validCsr());
        if (entitlement != null) {
            request.formField("entitlement", entitlement);
        }

        mockMvc.perform(request)
            .andExpectAll(
                status().isOk(),
                view().name("issuer_csr_form_view"),
                model().attributeHasFieldErrors(CsrController.CSR_FORM_ATTR, "entitlement"),
                model().attributeDoesNotExist(CsrController.NEW_CERTIFICATE_ATTR));

        verifyNoCertificateRequests();
    }

    @ParameterizedTest
    @CsvSource({"access, false", "issuer, false", "access, true", "issuer, true"})
    @DisplayName("Should deny another organisation access to CSR forms and issuance without certificate requests")
    void shouldDenyOtherOrganisation(String type, boolean submission) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);
        String path = "/csr/" + type + "/" + relyingParty.id();
        var request = submission
            ? post(path).formField("csrField", validCsr()).formField("entitlement", "pid_provider")
            : get(path);

        mockMvc.perform(request.with(csrf())
                .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno().equals("987654325") ? "123456785" : "987654325")))
            .andExpectAll(status().isNotFound(), view().name("error/404"));

        verify(mockRpService).get(relyingParty.id());
        verifyNoCertificateRequests();
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should reject certificate issuance without CSRF before calling the service")
    void shouldRejectIssuanceWithoutCsrf(String type) throws Exception {
        mockMvc.perform(post("/csr/" + type + "/" + UUID.randomUUID())
                .with(SecurityTestUtils.adminOidcLogin())
                .formField("csrField", validCsr())
                .formField("entitlement", "pid_provider"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(mockRpService);
    }

    private RequestPostProcessor login(RelyingPartyResource relyingParty, boolean administrator) {
        return administrator ? SecurityTestUtils.adminOidcLogin() : SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno());
    }

    private String validCsr() throws Exception {
        return PKCS10CertificationRequestConverter.toString(CertificatesGenerator.generatePKCS10Csr());
    }

    private void verifyNoCertificateRequests() {
        verify(mockRpService, never()).requestCertificateForRelyingParty(any(), any());
        verify(mockRpService, never()).requestIssuerCertificateForEntitlement(any(), any());
    }
}

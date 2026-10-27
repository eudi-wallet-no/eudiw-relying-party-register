package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.accesscertificates.X509CertificateConverter;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.AdminWebControllerAdvice;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the certificate download controller")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
public class DownloadCertificateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @ParameterizedTest
    @CsvSource({
        "access, -1, false", "access, 0, false", "access, -1, true",
        "issuer, -1, false", "issuer, 0, false", "issuer, 0, true"
    })
    @DisplayName("Should download active and revoked certificates as PEM for the owning organisation and administrators")
    void shouldDownloadCertificatePem(String type, int revocationStatus, boolean administrator) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        RelyingPartyCertificateResource generated = ResourceGenerator.generateCertificateResource();
        RelyingPartyCertificateResource certificate = new RelyingPartyCertificateResource(
            generated.certificate(), generated.id(), type.equals("issuer") ? "pid_provider" : null, revocationStatus);
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);
        if (type.equals("access")) {
            when(mockRpService.getAccessCertificate(relyingParty.id(), certificate.id())).thenReturn(certificate);
        } else {
            when(mockRpService.getIssuerCertificate(relyingParty.id(), certificate.id())).thenReturn(certificate);
        }

        mockMvc.perform(get(downloadPath(type, relyingParty.id(), certificate.id()))
                .with(administrator ? SecurityTestUtils.adminOidcLogin() : SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno())))
            .andExpectAll(
                status().isOk(),
                content().contentType("application/x-pem-file"),
                content().bytes(X509CertificateConverter.toPem(certificate.certificate()).getBytes()),
                header().string("Content-Disposition", "attachment; filename=\"" + certificate.certificate().getSerialNumber() + ".pem\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should show the requested certificate and relying party in the certificate-not-found page")
    void shouldShowCertificateNotFound(String type) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        UUID certificateId = UUID.randomUUID();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);
        var failure = new CertificateNotFoundException("Certificate not found", relyingParty.id().toString(), certificateId.toString());
        if (type.equals("access")) {
            when(mockRpService.getAccessCertificate(relyingParty.id(), certificateId)).thenThrow(failure);
        } else {
            when(mockRpService.getIssuerCertificate(relyingParty.id(), certificateId)).thenThrow(failure);
        }

        mockMvc.perform(get(downloadPath(type, relyingParty.id(), certificateId))
                .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno())))
            .andExpectAll(
                status().isNotFound(),
                view().name("error/certificate_not_found"),
                model().attribute(AdminWebControllerAdvice.requestedIdOrOrgnoAttrId, relyingParty.id().toString()),
                model().attribute(AdminWebControllerAdvice.certificateId, certificateId.toString()),
                header().doesNotExist("Content-Disposition"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should deny certificate download by another organisation without retrieving a certificate")
    void shouldDenyOtherOrganisation(String type) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);

        mockMvc.perform(get(downloadPath(type, relyingParty.id(), UUID.randomUUID()))
                .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno().equals("987654325") ? "123456785" : "987654325")))
            .andExpectAll(status().isNotFound(), view().name("error/404"), header().doesNotExist("Content-Disposition"));

        verify(mockRpService).get(relyingParty.id());
        verify(mockRpService, never()).getAccessCertificate(any(), any());
        verify(mockRpService, never()).getIssuerCertificate(any(), any());
    }

    private String downloadPath(String type, UUID relyingPartyId, UUID certificateId) {
        return "/get-certificate/" + relyingPartyId + (type.equals("issuer") ? "/issuer" : "") + "/" + certificateId;
    }
}

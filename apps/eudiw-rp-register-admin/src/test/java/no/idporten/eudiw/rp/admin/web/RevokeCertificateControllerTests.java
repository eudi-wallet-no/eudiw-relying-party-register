package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.security.SecurityTestUtils;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.exception.CertificateNotFoundException;
import no.idporten.eudiw.rp.admin.service.exception.ErrorResponseException;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.AdminWebControllerAdvice;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using the certificate revocation controller")
@AutoConfigureMockMvc
public class RevokeCertificateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelyingPartiesService mockRpService;

    @ParameterizedTest
    @CsvSource({"access, false", "issuer, false", "access, true", "issuer, true"})
    @DisplayName("Should revoke certificates and redirect the owning organisation and administrators to the certificate section")
    void shouldRevokeCertificateAndRedirect(String type, boolean administrator) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        UUID certificateId = UUID.randomUUID();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);

        mockMvc.perform(post(revokePath(type, relyingParty.id(), certificateId))
                .with(administrator ? SecurityTestUtils.adminOidcLogin() : SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno()))
                .with(csrf()))
            .andExpectAll(
                status().is3xxRedirection(),
                redirectedUrl("/details/" + relyingParty.id() + "#" + type + "-certificates"));

        if (type.equals("access")) {
            verify(mockRpService).revokeAccessCertificate(relyingParty.id(), certificateId);
            verify(mockRpService, never()).revokeIssuerCertificate(any(), any());
        } else {
            verify(mockRpService).revokeIssuerCertificate(relyingParty.id(), certificateId);
            verify(mockRpService, never()).revokeAccessCertificate(any(), any());
        }
    }

    @ParameterizedTest
    @CsvSource({"access, ca", "issuer, timeout", "access, response", "access, notfound"})
    @DisplayName("Should show revocation failures without redirecting to success")
    void shouldShowRevocationFailureWithoutRedirect(String type, String failureType) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        UUID certificateId = UUID.randomUUID();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);
        RuntimeException failure = switch (failureType) {
            case "ca" -> new ErrorResponseException("ca_error", "CA rejected revocation");
            case "timeout" -> new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"));
            case "response" -> new RestClientException("Unable to read response");
            default -> new CertificateNotFoundException("Certificate not found", relyingParty.id().toString(), certificateId.toString());
        };
        if (type.equals("access")) {
            doThrow(failure).when(mockRpService).revokeAccessCertificate(relyingParty.id(), certificateId);
        } else {
            doThrow(failure).when(mockRpService).revokeIssuerCertificate(relyingParty.id(), certificateId);
        }

        var result = mockMvc.perform(post(revokePath(type, relyingParty.id(), certificateId))
                .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno()))
                .with(csrf()))
            .andExpectAll(
                status().is(failureType.equals("notfound") ? 404 : 500),
                view().name(failureType.equals("notfound") ? "error/certificate_not_found" : "error/5xx"),
                header().doesNotExist("Location"),
                content().string(containsString(failureType.equals("notfound") ? "Sertifikat finst ikkje" : "Det har skjedd ein uventa feil")));
        if (failureType.equals("notfound")) {
            result.andExpectAll(
                model().attribute(AdminWebControllerAdvice.requestedIdOrOrgnoAttrId, relyingParty.id().toString()),
                model().attribute(AdminWebControllerAdvice.certificateId, certificateId.toString()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"access", "issuer"})
    @DisplayName("Should deny certificate revocation by another organisation without revoking a certificate")
    void shouldDenyOtherOrganisation(String type) throws Exception {
        RelyingPartyResource relyingParty = ResourceGenerator.generateRelyingPartyResource();
        when(mockRpService.get(relyingParty.id())).thenReturn(relyingParty);

        mockMvc.perform(post(revokePath(type, relyingParty.id(), UUID.randomUUID()))
                .with(SecurityTestUtils.oidcLoginForOrgno(relyingParty.orgno().equals("987654325") ? "123456785" : "987654325"))
                .with(csrf()))
            .andExpectAll(status().isNotFound(), view().name("error/404"), header().doesNotExist("Location"));

        verify(mockRpService).get(relyingParty.id());
        verify(mockRpService, never()).revokeAccessCertificate(any(), any());
        verify(mockRpService, never()).revokeIssuerCertificate(any(), any());
    }

    private String revokePath(String type, UUID relyingPartyId, UUID certificateId) {
        return "/revoke-certificate/" + relyingPartyId + "/" + type + "/" + certificateId;
    }
}

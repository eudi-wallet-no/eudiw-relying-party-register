package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.service.accesscertificates.X509CertificateConverter;
import no.idporten.eudiw.rp.admin.service.exception.NotFoundException;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.web.resource.RelyingPartyResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the certificate download controller")
@AutoConfigureMockMvc(addFilters = false)
public class DownloadCertificateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("When GET'ing the certificate download endpoint")
    class DownloadCertificateEndpointGetTests {

        @Test
        @DisplayName("then expected certificate is downloaded")
        public void testCorrectCertificateDownloadedWhenExists() throws Exception {

            RelyingPartyCertificateResource certResource =
                ResourceGenerator.generateCertificateResource();
            RelyingPartyResource rpResource = ResourceGenerator.generateRelyingPartyResource();
            UUID rpId = rpResource.id();
            UUID certId = UUID.randomUUID();
            when(mockRpService.getCertificate(rpId, certId)).thenReturn(certResource);
            when(mockRpService.get(rpId)).thenReturn(rpResource);

            String expectedContentType = "application/x-pem-file";
            byte[] expectedContentBytes =
                X509CertificateConverter.toPem(certResource.certificate())
                                        .getBytes();

            mockMvc.perform(get("/get-certificate/%s/%s".formatted(rpId, certId)))
                .andExpectAll(
                    status().isOk(),
                    content().contentType(expectedContentType),
                    content().bytes(expectedContentBytes)
                );
        }

        @Test
        @DisplayName("then 404 view shown when certificate not exists")
        public void test404PageShownWhenCertificateNotExists() throws Exception {
            when(mockRpService.getCertificate(any(), any()))
                .thenThrow(NotFoundException.class);
            when(mockRpService.get(any()))
                .thenReturn(ResourceGenerator.generateRelyingPartyResource());

            UUID rpId = UUID.randomUUID();
            UUID certId = UUID.randomUUID();

            mockMvc.perform(get("/get-certificate/%s/%s".formatted(rpId, certId)))
                   .andExpect(view().name("error/404"));
        }
    }
}

package no.eudiw.rp.register.api;


import no.eudiw.rp.register.api.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.service.certificateservice.RelyingPartyCertificateService;
import no.eudiw.rp.register.service.certificateservice.RevocationRequest;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.ResourceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the relying party certificates API : issuer")
@ActiveProfiles("junit")
public class IssuerCertificateControllerTests {

    public static final String X_API_KEY_HEADER = "X-API-KEY";
    public static final String VALID_API_KEY = "junit-api-key";

    @Autowired
    private RelyingPartyInstanceRepository relyingPartyInstanceRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelyingPartyCertificateService mockCsrService;

    @BeforeEach
    void clearRepositoryBeforeEachTest() {
        relyingPartyInstanceRepository.deleteAll();
    }

    @BeforeEach
    void setupMockCsrService() {
        RelyingPartyCertificateResource dummyCertificateResource =
                ResourceGenerator.generateRelyingPartyCertificateResource();

        when(mockCsrService.requestIssuerCertificate(any(), any()))
                .thenReturn(dummyCertificateResource);

        when(mockCsrService.getAllIssuerCertificatesFromRelyingParty(any()))
                .thenReturn(new RelyingPartyCertificatesResource(List.of(dummyCertificateResource)));

        when(mockCsrService.getIssuerCertificate(any(), any()))
                .thenReturn(dummyCertificateResource);
    }

    @Nested
    @DisplayName("When reading issuer certificates for a specific relying party ...")
    class GetIssuerCertificatesEndpointTests {

        @Test
        @DisplayName("then the relying party's issuer ´certificates are retrieved and returned")
        public void testEndpointCallsServiceWithCorrectArguments() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/v1/rp/%s/certs/issuer".formatted(id))
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.certificates").exists())
                    .andExpect(jsonPath("$.certificates").isArray());

            verify(mockCsrService, times(1)).getAllIssuerCertificatesFromRelyingParty(id);
        }

        @Test
        @DisplayName("then a 400 error response resource is returned on invalid ID")
        public void test404ErrorResponseReturnedOnInvalidRelyingPartyId() throws Exception {
            mockMvc.perform(get("/v1/rp/invalid_id/certs/issuer")
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.certificates").doesNotExist())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error").value("invalid_request"))
                    .andExpect(jsonPath("$.error_description").exists());
        }
    }

    @Nested
    @DisplayName("When reading a specific issuer certificate by its ID and ID of its holder ...")
    class GetCertificateEndpointTests {

        @Test
        @DisplayName("then the endpoint returns certificate if both IDs exist")
        public void testEndpointCallsServiceWithCorrectArguments() throws Exception {

            UUID certificateId = UUID.randomUUID();
            UUID relyingPartyId = UUID.randomUUID();
            String requestUrl = "/v1/rp/%s/certs/issuer/%s".formatted(relyingPartyId, certificateId);
            mockMvc.perform(get(requestUrl)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.certificate").exists());

            verify(mockCsrService, times(1)).getIssuerCertificate(certificateId, relyingPartyId);
        }

        @Test
        @DisplayName("then a 400 error response resource is returned on invalid ID(s)")
        public void test400ErrorResponseReturnedOnInvalidIds() throws Exception {
            UUID validId = UUID.randomUUID();
            String invalidIdStr = "foo";
            mockMvc.perform(get("/v1/rp/%s/certs/issuer/%s".formatted(validId, invalidIdStr))
                            .accept(MediaType.APPLICATION_JSON_VALUE)
                            .header(X_API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.certificates").doesNotExist())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error").value("invalid_request"))
                    .andExpect(jsonPath("$.error_description").exists());

            verifyNoInteractions(mockCsrService);
        }
    }

    @Nested
    @DisplayName("when requesting new ISSUER certificates for RPs ...")
    class RequestIssuerCertificateTests {

        @Test
        @DisplayName("then service called with correct arguments if ID and CSR valid")
        public void testEndpointCallsServiceWithCorrectArguments() throws Exception {
            UUID validId = UUID.randomUUID();
            IssuerCsrResource dummyCsrResource = new IssuerCsrResource(CertificatesGenerator.generatePKCS10Csr(),
                    "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider");
            String validContent = new ObjectMapper().writeValueAsString(dummyCsrResource);

            mockMvc.perform(post("/v1/rp/%s/certs/issuer".formatted(validId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON_VALUE, "application/x-pem-file")
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(validContent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.certificate").exists());

            verify(mockCsrService, times(1)).requestIssuerCertificate(
                    validId, dummyCsrResource);
        }

        @Test
        @DisplayName("then an invalid ID gives 400 and no interactions with service")
        public void test400ErrorResponseReturnedOnInvalidID() throws Exception {
            String invalidId = "foo";
            IssuerCsrResource dummyCsrResource = new IssuerCsrResource(CertificatesGenerator.generatePKCS10Csr(),
                    "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider");
            String validContent = new ObjectMapper().writeValueAsString(dummyCsrResource);

            mockMvc.perform(post("/v1/rp/%s/certs/issuer".formatted(invalidId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON_VALUE, "application/x-pem-file")
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(validContent))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.certificate").doesNotExist())
                    .andExpect(jsonPath("$.certificates").doesNotExist())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error").value("invalid_request"))
                    .andExpect(jsonPath("$.error_description").exists());

            verifyNoInteractions(mockCsrService);
        }


        @Test
        @DisplayName("then an invalid CSR body gives 400 and no interactions with service")
        public void test400ErrorResponseReturnedOnInvalidCSR() throws Exception {
            UUID validId = UUID.randomUUID();
            IssuerCsrResource dummyCsrResource = new IssuerCsrResource(CertificatesGenerator.generatePKCS10Csr(),
                    "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider");
            String validContent = new ObjectMapper().writeValueAsString(dummyCsrResource);
            String invalidContent = validContent.toLowerCase();

            mockMvc.perform(post("/v1/rp/%s/certs/issuer".formatted(validId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(invalidContent))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.certificate").doesNotExist())
                    .andExpect(jsonPath("$.certificates").doesNotExist())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error").value("invalid_request"))
                    .andExpect(jsonPath("$.error_description").exists());

            verifyNoInteractions(mockCsrService);
        }
    }

    @Nested
    @DisplayName("when revoking certificates ..")
    class RevokeCertificatesTests {

        @Test
        @DisplayName("then service is called and result is 2xx success when given RpId has" +
                " an issuer certificate with given certId which is not previously revoked")
        public void test204whenRevokingIssuerCertificateSuccessfully() throws Exception {
            UUID relyingPartyId = UUID.randomUUID();
            RevocationRequest request = new RevocationRequest();
            request.setReason(0);
            UUID certificateId = UUID.randomUUID();
            String serialNumber = "12345678910";
            request.setSerialNumber(serialNumber);
            String validContent = new ObjectMapper().writeValueAsString(request);
            mockMvc.perform(patch("/v1/rp/%s/certs/issuer/%s/revoke".formatted(relyingPartyId, certificateId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(validContent))
                    .andExpect(status().is2xxSuccessful());

            verify(mockCsrService, times(1)).revokeIssuerCertificate(certificateId, relyingPartyId);
        }
    }


}

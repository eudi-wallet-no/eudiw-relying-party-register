package no.eudiw.rp.register.api;

import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.service.certificateservice.RelyingPartyCertificateService;
import no.eudiw.rp.register.service.certificateservice.RevocationRequest;
import no.eudiw.rp.register.testdata.ResourceGenerator;
import org.junit.jupiter.api.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When using the relying party certificates API : access")
@ActiveProfiles("junit")
public class AccessCertificateControllerTests {

    public static final String X_API_KEY_HEADER = "X-API-KEY";
    public static final String VALID_API_KEY = "junit-api-key";

    @Autowired
    private RelyingPartyInstanceRepository relyingPartyRepository;

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartyCertificateService mockCsrService;

    @BeforeEach
    void clearRepositoryBeforeEachTest() {
        relyingPartyRepository.deleteAll();
    }

    @BeforeEach
    void setupMockCsrService() throws Exception {
        RelyingPartyCertificateResource dummyCertificateResource =
            ResourceGenerator.generateRelyingPartyCertificateResource();

        when(mockCsrService.requestAccessCertificateForRelyingParty(any(), any()))
            .thenReturn(dummyCertificateResource);

        when(mockCsrService.getCertificatesForRelyingParty(any()))
            .thenReturn(new RelyingPartyCertificatesResource(List.of(dummyCertificateResource)));

        when(mockCsrService.getCertificate(any(), any()))
            .thenReturn(dummyCertificateResource);
    }

    @Nested
    @DisplayName("When reading access certificates for a specific relying party ...")
    class GetCertificatesEndpointTests {

        @Test
        @DisplayName("then the endpoint uses the service and returns the expected resource")
        public void testEndpointCallsServiceWithCorrectArguments() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/v1/rp/%s/certs/access".formatted(id))
                                .accept(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificates").exists())
                .andExpect(jsonPath("$.certificates").isArray());

            verify(mockCsrService, times(1)).getCertificatesForRelyingParty(id);
        }

        @Test
        @DisplayName("then a 400 error response resource is returned on invalid ID")
        public void test404ErrorResponseReturnedOnInvalidRelyingPartyId() throws Exception {
            mockMvc.perform(get("/v1/rp/invalid_id/certs/access")
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
    @DisplayName("When reading a specific access certificate by its ID and ID of its holder ...")
    class GetCertificateEndpointTests {

        @Test
        @DisplayName("then the endpoint returns certificate if both IDs exist")
        public void testEndpointCallsServiceWithCorrectArguments() throws Exception {

            UUID certificateId = UUID.randomUUID();
            UUID relyingPartyId = UUID.randomUUID();
            String requestUrl = "/v1/rp/%s/certs/access/%s".formatted(relyingPartyId, certificateId);
            mockMvc.perform(get(requestUrl)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(X_API_KEY_HEADER, VALID_API_KEY))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.certificate").exists());

            verify(mockCsrService, times(1)).getCertificate(certificateId, relyingPartyId);
        }

        @Test
        @DisplayName("then a 400 error response resource is returned on invalid ID(s)")
        public void test400ErrorResponseReturnedOnInvalidIds() throws Exception {
            UUID validId = UUID.randomUUID();
            String invalidIdStr = "foo";
            mockMvc.perform(get("/v1/rp/%s/certs/access/%s".formatted(validId, invalidIdStr))
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
    @DisplayName("when requesting new ACCESS certificates for RPs ...")
    class RequestAccessCertificateTests {

        @Test
        @DisplayName("then service called with correct arguments if ID and CSR valid")
        public void testEndpointCallsServiceWithCorrectArguments() throws Exception {
            UUID validId = UUID.randomUUID();
            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            String validContent = new ObjectMapper().writeValueAsString(dummyCsrResource);

            mockMvc.perform(post("/v1/rp/%s/certs/access".formatted(validId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON_VALUE, "application/x-pem-file")
                                .header(X_API_KEY_HEADER, VALID_API_KEY)
                                .content(validContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificate").exists());

            verify(mockCsrService, times(1)).requestAccessCertificateForRelyingParty(validId, dummyCsrResource);
        }

        @Test
        @DisplayName("then an invalid ID gives 400 and no interactions with service")
        public void test400ErrorResponseReturnedOnInvalidID() throws Exception {
            String invalidId = "foo";
            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            String validContent = new ObjectMapper().writeValueAsString(dummyCsrResource);

            mockMvc.perform(post("/v1/rp/%s/certs/access".formatted(invalidId))
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
            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            String validContent = new ObjectMapper().writeValueAsString(dummyCsrResource);
            String invalidContent = validContent.toLowerCase();

            mockMvc.perform(post("/v1/rp/%s/certs/access".formatted(validId))
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
        @DisplayName("When revoking access certificate")
        public void test204whenRevokingAccessCertificateSuccessfully() throws Exception {
            UUID relyingPartyId = UUID.randomUUID();
            RevocationRequest request = new RevocationRequest();
            request.setReason(0);
            UUID certificateId = UUID.randomUUID();
            String serialNumber = "12345678910";
            request.setSerialNumber(serialNumber);
            String validContent = new ObjectMapper().writeValueAsString(request);
            mockMvc.perform(patch("/v1/rp/%s/certs/access/%s/revoke".formatted(relyingPartyId, certificateId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_API_KEY_HEADER, VALID_API_KEY)
                            .content(validContent))
                    .andExpect(status().is2xxSuccessful());

            verify(mockCsrService, times(1)).revokeAccessCertificate(certificateId, relyingPartyId);
        }
    }
}

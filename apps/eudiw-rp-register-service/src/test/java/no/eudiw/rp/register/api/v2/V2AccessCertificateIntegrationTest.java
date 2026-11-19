package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.api.v2.endpoints.entities.AccessCertificateRequest;
import no.eudiw.rp.register.api.v2.endpoints.entities.CertificateResponse;
import no.eudiw.rp.register.domain.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.domain.certificates.X509CertificateConverter;
import no.eudiw.rp.register.repository.AccessCertificateRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V2AccessCertificateIntegrationTest {

    private static final MockWebServer CA_SERVER = new MockWebServer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @Autowired
    private AccessCertificateRepository accessCertificateRepository;

    @DynamicPropertySource
    static void configureCa(DynamicPropertyRegistry registry) {
        registry.add("TEST_MOCK_CA_SERVER_PORT", CA_SERVER::getPort);
    }

    @AfterAll
    static void stopCa() throws IOException {
        CA_SERVER.close();
    }

    @Test
    @DisplayName("Should return and persist the access certificate issued by the CA")
    void shouldReturnAndPersistTheAccessCertificateIssuedByTheCa() throws Exception {
        var party = walletRelyingPartyRepository.saveAndFlush(EntityGenerator.generateWalletRelyingParty(1));
        var instanceId = EntityGenerator.instances(party).getFirst().getId();
        var csr = CertificatesGenerator.generatePKCS10Csr();
        var certificate = CertificatesGenerator.generateX509Certificate();
        CA_SERVER.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/x-pem-file")
            .setBody(X509CertificateConverter.convert(certificate)));

        try {
            var result = mockMvc.perform(post("/v2/instances/{instanceId}/certificates/access", instanceId)
                    .header("X-API-KEY", "junit-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AccessCertificateRequest(csr))))
                .andExpect(status().isOk())
                .andReturn();

            var caRequest = CA_SERVER.takeRequest(5, TimeUnit.SECONDS);
            assertNotNull(caRequest);
            assertEquals("POST", caRequest.getMethod());
            assertEquals("/v1/internal/issue/junitaccess1", caRequest.getPath());
            var caBody = objectMapper.readTree(caRequest.getBody().readUtf8());
            assertEquals(PKCS10CertificationRequestConverter.convert(csr), caBody.get("csr").asString());

            var response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), CertificateResponse.class);
            assertNotNull(response.id());
            assertEquals(certificate, response.certificate());
            var stored = accessCertificateRepository.findByIdAndRelyingPartyId(response.id(), instanceId).orElseThrow();
            assertEquals(certificate, stored.getCertificate());
        } finally {
            walletRelyingPartyRepository.deleteById(party.getId());
        }
    }

    @ParameterizedTest
    @MethodSource("caIssuanceFailures")
    @DisplayName("Should not store an access certificate when the CA response is invalid")
    void shouldNotStoreAnAccessCertificateWhenTheCaResponseIsInvalid(
        int caStatus,
        String contentType,
        String body,
        int expectedStatus
    ) throws Exception {
        var party = walletRelyingPartyRepository.saveAndFlush(EntityGenerator.generateWalletRelyingParty(1));
        var instanceId = EntityGenerator.instances(party).getFirst().getId();
        long certificateCount = accessCertificateRepository.count();

        var caResponse = new MockResponse()
            .setResponseCode(caStatus)
            .setHeader("Content-Type", contentType);
        if (body != null) {
            caResponse.setBody(body);
        }
        CA_SERVER.enqueue(caResponse);

        try {
            mockMvc.perform(post("/v2/instances/{instanceId}/certificates/access", instanceId)
                    .header("X-API-KEY", "junit-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new AccessCertificateRequest(CertificatesGenerator.generatePKCS10Csr()))))
                .andExpect(status().is(expectedStatus));

            assertNotNull(CA_SERVER.takeRequest(5, TimeUnit.SECONDS));
            assertEquals(certificateCount, accessCertificateRepository.count());
        } finally {
            walletRelyingPartyRepository.deleteById(party.getId());
        }
    }

    private static Stream<Arguments> caIssuanceFailures() {
        String invalidCertificatePem = X509CertificateConverter
            .convert(CertificatesGenerator.generateX509Certificate())
            .toLowerCase();

        return Stream.of(
            Arguments.of(400, "application/json", "{\"error\":\"invalid_request\"}", 500),
            Arguments.of(200, "application/x-pem-file", invalidCertificatePem, 400),
            Arguments.of(200, "application/x-pem-file", null, 500)
        );
    }
}

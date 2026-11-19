package no.eudiw.rp.register.api.v2;

import no.eudiw.rp.register.api.v2.endpoints.entities.CertificateResponse;
import no.eudiw.rp.register.api.v2.endpoints.entities.IssuerCertificateRequest;
import no.eudiw.rp.register.domain.Entitlement;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.domain.certificates.X509CertificateConverter;
import no.eudiw.rp.register.repository.EntitlementRepository;
import no.eudiw.rp.register.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("junit")
class V2IssuerCertificateIntegrationTest {

    private static final String ENTITLEMENT = "urn:test:v2:issuer-certificate";
    private static final String CA_ID = "junit-issuer-ca";
    private static final MockWebServer CA_SERVER = new MockWebServer();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private IssuerCertificateRepository issuerCertificateRepository;

    @DynamicPropertySource
    static void configureCa(DynamicPropertyRegistry registry) {
        registry.add("TEST_MOCK_CA_SERVER_PORT", CA_SERVER::getPort);
    }

    @AfterAll
    static void stopCa() throws IOException {
        CA_SERVER.close();
    }

    @Test
    @DisplayName("Should issue and store an issuer certificate for its entitlement")
    void shouldIssueAndStoreAnIssuerCertificateForItsEntitlement() throws Exception {
        var entitlement = entitlementRepository.saveAndFlush(new Entitlement(ENTITLEMENT, true, "Issuer", CA_ID));
        var instance = new RelyingPartyInstance(
            List.of(new RelyingPartyEntitlement(ENTITLEMENT)), List.of(), List.of());
        var service = new WalletRelyingPartyService("Service", null, List.of(instance));
        service.setEntitlements(List.of(ENTITLEMENT));
        var party = walletRelyingPartyRepository.saveAndFlush(new WalletRelyingParty(
            "Party", no.eudiw.rp.register.testdata.TestDataGenerator.generateValidOrgno(), true, List.of(service)));
        var csr = CertificatesGenerator.generatePKCS10Csr();
        var certificate = CertificatesGenerator.generateX509Certificate();
        CA_SERVER.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/x-pem-file")
            .setBody(X509CertificateConverter.convert(certificate)));

        try {
            var result = mockMvc.perform(post("/v2/instances/{instanceId}/certificates/issuer", instance.getId())
                    .header("X-API-KEY", "junit-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new IssuerCertificateRequest(csr, ENTITLEMENT))))
                .andExpect(status().isOk())
                .andReturn();

            var caRequest = CA_SERVER.takeRequest(5, TimeUnit.SECONDS);
            assertNotNull(caRequest);
            assertEquals("/v1/internal/issue/" + CA_ID, caRequest.getPath());
            var caBody = objectMapper.readTree(caRequest.getBody().readUtf8());
            assertEquals(PKCS10CertificationRequestConverter.convert(csr), caBody.get("csr").asString());

            var response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), CertificateResponse.class);
            assertNotNull(response.id());
            assertEquals(ENTITLEMENT, response.entitlement());
            assertEquals(certificate, response.certificate());
            var stored = issuerCertificateRepository.findById(response.id()).orElseThrow();
            assertEquals(certificate, stored.getCertificate());
            assertEquals(ENTITLEMENT, stored.getEntitlement().getEntitlement());
        } finally {
            walletRelyingPartyRepository.deleteById(party.getId());
            entitlementRepository.delete(entitlement);
        }
    }
}

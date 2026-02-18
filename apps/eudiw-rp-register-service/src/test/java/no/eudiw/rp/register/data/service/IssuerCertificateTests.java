package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.entity.certificates.X509CertificateConverter;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.data.repository.LegalEntityRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.exception.RegisterServiceException;
import no.eudiw.rp.register.service.certificateservice.RelyingPartyCertificateService;
import no.eudiw.rp.register.service.exception.NotFoundException;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("Issuer certificate tests")
public class IssuerCertificateTests {

    @Autowired
    private RelyingPartyCertificateService certService;

    @Autowired
    private RelyingPartyInstanceRepository instanceRepository;

    @Autowired
    private LegalEntityRepository rpRepository;

    @Nested
    @DisplayName("when registering a new issuer certificate")
    class RegisterIssuerCertificatesForEntitlementsTests {

        @Value("${TEST_MOCK_CA_SERVER_PORT}")
        int caServicePort;

        private MockWebServer mockCaServer;


        @BeforeEach
        void setup() throws IOException {
            rpRepository.deleteAll();
            instanceRepository.deleteAll();
            mockCaServer = new MockWebServer();
            mockCaServer.start(caServicePort);
        }

        @AfterEach
        void cleanUp() throws IOException {
            mockCaServer.close();
        }


        private X509Certificate enqueueMockCertificateResponse() {
            X509Certificate certificate = CertificatesGenerator.generateX509Certificate();
            String certificateInPem = X509CertificateConverter.convert(certificate);
            MockResponse mockValidCertificateResponse =
                    new MockResponse()
                            .setResponseCode(200)
                            .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-pem-file")
                            .setBody(certificateInPem);
            mockCaServer.enqueue(mockValidCertificateResponse);
            return certificate;
        }

        @Test
        @DisplayName("cert request is denied for issuer entitlement")
        public void test_Issuer_RegisterCert() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider")));

            rpRepository.saveAndFlush(legalEntity);

            assertThrows(RegisterServiceException.class,
                    () -> certService.requestIssuerCertificate(
                            legalEntity.getRelyingPartyInstances().getFirst().getId(),
                            new IssuerCsrResource(
                                    CertificatesGenerator.generatePKCS10Csr(),
                                    "https://uri.etsi.org/19475/Entitlement/Service_Provider"
                            )
                    ));
        }

        @Test
        @DisplayName("cert is saved to eaa_provider entitlement")
        public void test_EAA_Provider_RegisterCert() throws Exception {
            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(
                    List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")));
            rpRepository.saveAndFlush(legalEntity);

            X509Certificate certificateActual =
                    certService.requestIssuerCertificate(
                            legalEntity.getRelyingPartyInstances().getFirst().getId(),
                            new IssuerCsrResource(
                                    CertificatesGenerator.generatePKCS10Csr(),
                                    "https://uri.etsi.org/19475/Entitlement/QEAA_Provider"
                            )
                    ).certificate();

            assertEquals(certificateExpected, certificateActual);
            RecordedRequest recordedRequest = mockCaServer.takeRequest();
            assertAll(
                    () -> assertEquals("POST", recordedRequest.getMethod()),
                    () -> assertEquals("/v1/certs/eaa_provider", recordedRequest.getPath())
            );

            assertTrue(instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).isPresent(), "legal entity with at least one RelyingPartyInstances should be present in repository");
            RelyingPartyInstance resultRp = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).get();
            RelyingPartyEntitlement entitlement = resultRp.getRelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider").get();
            assertNotNull(entitlement);
            assertNotNull(entitlement.getIssuerCertificates());
            assertEquals(1, entitlement.getIssuerCertificates().size());
        }

        @Test
        @DisplayName("cert is saved to Non_Q_EAA_Provider entitlement")
        public void test_Non_Q_EAA_Provider_RegisterCert() throws Exception {
            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(
                    List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider")));
            rpRepository.saveAndFlush(legalEntity);

            X509Certificate certificateActual =
                    certService.requestIssuerCertificate(
                            legalEntity.getRelyingPartyInstances().getFirst().getId(),
                            new IssuerCsrResource(
                                    CertificatesGenerator.generatePKCS10Csr(),
                                    "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider"
                            )
                    ).certificate();

            assertEquals(certificateExpected, certificateActual);
            RecordedRequest recordedRequest = mockCaServer.takeRequest();
            assertAll(
                    () -> assertEquals("POST", recordedRequest.getMethod()),
                    () -> assertEquals("/v1/certs/eaa_provider", recordedRequest.getPath())
            );

            assertTrue(instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).isPresent(), "legal entity with at least one RelyingPartyInstances should be present in repository");
            RelyingPartyInstance resultRp = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).get();
            RelyingPartyEntitlement entitlement = resultRp.getRelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider").get();
            assertNotNull(entitlement);
            assertNotNull(entitlement.getIssuerCertificates());
            assertEquals(1, entitlement.getIssuerCertificates().size());
        }

        @Test
        @DisplayName("cert is saved to PUB_EAA_Provider entitlement")
        public void test_PUB_EAA_Provider_RegisterCert() throws Exception {
            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();

            legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(
                    List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider")));
            rpRepository.saveAndFlush(legalEntity);

            X509Certificate certificateActual =
                    certService.requestIssuerCertificate(
                            legalEntity.getRelyingPartyInstances().getFirst().getId(),
                            new IssuerCsrResource(
                                    CertificatesGenerator.generatePKCS10Csr(),
                                    "https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider"
                            )
                    ).certificate();

            assertEquals(certificateExpected, certificateActual);
            RecordedRequest recordedRequest = mockCaServer.takeRequest();
            assertAll(
                    () -> assertEquals("POST", recordedRequest.getMethod()),
                    () -> assertEquals("/v1/certs/eaa_provider", recordedRequest.getPath())
            );

            assertTrue(instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).isPresent(), "legal entity with at least one RelyingPartyInstances should be present in repository");
            RelyingPartyInstance resultRp = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).get();
            RelyingPartyEntitlement entitlement = resultRp.getRelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider").get();
            assertNotNull(entitlement);
            assertNotNull(entitlement.getIssuerCertificates());
            assertEquals(1, entitlement.getIssuerCertificates().size());
        }

        @Test
        @DisplayName("cert is saved to PID_Provider entitlement")
        public void test_PID_Provider_RegisterCert() throws Exception {
            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setRelyingPartyEntitlements(
                    List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/PID_Provider")));
            rpRepository.saveAndFlush(legalEntity);

            X509Certificate certificateActual =
                    certService.requestIssuerCertificate(
                            legalEntity.getRelyingPartyInstances().getFirst().getId(),
                            new IssuerCsrResource(
                                    CertificatesGenerator.generatePKCS10Csr(),
                                    "https://uri.etsi.org/19475/Entitlement/PID_Provider"
                            )
                    ).certificate();

            assertEquals(certificateExpected, certificateActual);
            RecordedRequest recordedRequest = mockCaServer.takeRequest();
            assertAll(
                    () -> assertEquals("POST", recordedRequest.getMethod()),
                    () -> assertEquals("/v1/certs/pid_provider", recordedRequest.getPath())
            );

            assertTrue(instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).isPresent(), "legal entity with at least one RelyingPartyInstances should be present in repository");
            RelyingPartyInstance resultRp = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).get();
            RelyingPartyEntitlement entitlement = resultRp.getRelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/PID_Provider").get();
            assertNotNull(entitlement);
            assertNotNull(entitlement.getIssuerCertificates());
            assertEquals(1, entitlement.getIssuerCertificates().size());
        }

        @Test
        @DisplayName("then CSR is rejected for inactive RP instance")
        public void testIssuerCsrRejectedForInactiveRelyingParty() {
            RelyingPartyInstance relyingPartyInstance = EntityGenerator.generateRelyingParty();
            relyingPartyInstance.setActive(false);
            instanceRepository.saveAndFlush(relyingPartyInstance);

            String entitlement = relyingPartyInstance.getRelyingPartyEntitlements().getFirst().getEntitlement();
            IssuerCsrResource csrResource = new IssuerCsrResource(CertificatesGenerator.generatePKCS10Csr(), entitlement);

            assertThrows(
                    NotFoundException.class,
                    () -> certService.requestIssuerCertificate(relyingPartyInstance.getId(), csrResource)
            );
        }
    }
}

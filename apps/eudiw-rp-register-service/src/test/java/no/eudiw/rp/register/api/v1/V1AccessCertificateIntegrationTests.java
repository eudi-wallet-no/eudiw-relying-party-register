package no.eudiw.rp.register.api.v1;

import no.eudiw.rp.register.api.resource.ErrorResponseResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.v1.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.domain.LegalEntity;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.X509CertificateConverter;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.AccessCertificateRepository;
import no.eudiw.rp.register.repository.LegalEntityRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.exception.CertificateConversionException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import no.eudiw.rp.register.exception.ErrorResponseException;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.ResourceGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using access certificates through the V1 API service")
public class V1AccessCertificateIntegrationTests {

    @Autowired
    private V1ApiService certService;

    @Autowired
    private AccessCertificateRepository certRepository;

    @Autowired
    private RelyingPartyInstanceRepository instanceRepository;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    @BeforeEach
    public void clearRepositoryBeforeEachTest() {
        instanceRepository.deleteAll();
        legalEntityRepository.deleteAll();
    }

    @Nested
    @DisplayName("when reading certificates for a relying party ...")
    class GetCertificatesForRelyingPartyTests {

        @Test
        @DisplayName("then only certificates for the requested RP are returned")
        public void testGetCertificatesForRelyingParty() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));

            LegalEntity otherLegalEntity = EntityGenerator.generateLegalEntity();
            otherLegalEntity.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));

            legalEntityRepository.saveAllAndFlush(List.of(legalEntity, otherLegalEntity));

            Set<X509Certificate> certsExpected =
                legalEntity.getRelyingPartyInstances().getFirst().getAccessCertificates()
                            .stream()
                            .map(AccessCertificate::getCertificate)
                            .collect(Collectors.toSet());

            Set<X509Certificate> certsActual =
                certService.getCertificatesForRelyingParty(legalEntity.getRelyingPartyInstances().getFirst().getId())
                           .certificates()
                           .stream()
                           .map(RelyingPartyCertificateResource::certificate)
                           .collect(Collectors.toSet());

            assertEquals(certsExpected, certsActual);
        }

        @Test
        @DisplayName("then service throws 404 if RP does not exist")
        public void testNoCertificatesReturnedForNonexistentRelyingParty() {
            assertThrows(NotFoundException.class,
                         () -> certService.getCertificatesForRelyingParty(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("when reading a specific certificate by its ID and ID of its holder ...")
    class GetCertificateTests {

        @Test
        @DisplayName("then certificate is returned if its ID is registered for RP")
        public void testCertificateReturnedIfExistsAndBelongsToRelyingParty() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));
            legalEntityRepository.saveAndFlush(legalEntity);

            AccessCertificate certificate =
                legalEntity.getRelyingPartyInstances().getFirst().getAccessCertificates().getFirst();

            // would throw on unknown ID(s)
            X509Certificate certificateActual =
                certService.getCertificate(certificate.getId(), legalEntity.getRelyingPartyInstances().getFirst().getId())
                           .certificate();

            X509Certificate certificateExpected = certificate.getCertificate();
            assertEquals(certificateExpected, certificateActual);
        }

        @Test
        @DisplayName("then the service throws if RP ID is known but certificate ID is unknown")
        public void testErrorThrownIfRelyingPartyKnownButCertificateUnknown() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));
            legalEntityRepository.saveAndFlush(legalEntity);

            UUID unknownCertificateId = UUID.randomUUID();
            UUID knownRelyingPartyId = legalEntity.getRelyingPartyInstances().getFirst().getId();

            assertThrows(
                NotFoundException.class,
                () -> certService.getCertificate(unknownCertificateId, knownRelyingPartyId)
            );
        }

        @Test
        @DisplayName("then the service throws if certificate ID is known but RP ID is unknown")
        public void testErrorThrownIfCertificateKnownButRelyingPartyUnknown() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));
            legalEntityRepository.saveAndFlush(legalEntity);

            UUID knownCertificateId = legalEntity.getRelyingPartyInstances().getFirst().getAccessCertificates().getFirst().getId();
            UUID unknownRelyingPartyId = UUID.randomUUID();
            assertThrows(
                NotFoundException.class,
                () -> certService.getCertificate(knownCertificateId, unknownRelyingPartyId)
            );
        }

        @Test
        @DisplayName("then service throws if certificate and RP known but certificate held by different RP")
        public void testErrorThrownIfCertificateExistsForDifferentRelyingParty() {
            LegalEntity legalEntity1 = EntityGenerator.generateLegalEntity();
            legalEntity1.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));
            legalEntityRepository.saveAndFlush(legalEntity1);

            LegalEntity legalEntity2 = EntityGenerator.generateLegalEntity();
            legalEntity2.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));
            legalEntityRepository.saveAndFlush(legalEntity2);

            UUID knownCertificateId = legalEntity1.getRelyingPartyInstances().getFirst().getAccessCertificates().getFirst().getId();
            UUID knownRelyingPartyId = legalEntity2.getRelyingPartyInstances().getFirst().getId();
            assertThrows(
                NotFoundException.class,
                () -> certService.getCertificate(knownCertificateId, knownRelyingPartyId)
            );
        }
    }

    @Nested
    @DisplayName("When requesting new certificates for relying parties ...")
    class RequestCertificateForRelyingPartyTests {

        @Value("${TEST_MOCK_CA_SERVER_PORT}")
        int caServicePort;

        private MockWebServer mockCaServer;

        @BeforeEach
        public void setup() throws Exception {
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

        private void enqueueMockRevocationCall() {
            mockCaServer.enqueue(new MockResponse().setBody("")
                    .setResponseCode(204));
        }

        @Test
        @DisplayName("then X509 certificates are properly deserialized at the rest client")
        public void testCorrectDeserializationOfValidCertificateResponse() throws Exception {
            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntity.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(EntityGenerator.generateCertificate()));

            UUID registreeId = legalEntityRepository.save(legalEntity).getRelyingPartyInstances().getFirst().getId();

            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            X509Certificate certificateActual =
                certService.requestAccessCertificateForRelyingParty(registreeId, dummyCsrResource)
                           .certificate();

            assertEquals(certificateExpected, certificateActual);
            RecordedRequest recordedRequest = mockCaServer.takeRequest();
            assertAll(
                    () -> assertEquals("POST", recordedRequest.getMethod()),
                    () -> assertEquals("/v1/certs/junitaccess1", recordedRequest.getPath())
            );
        }


        @Test
        @DisplayName("then the certificate returned by CA is properly stored in the database")
        public void testCertificateFromCAProperlyStoredInRegisterServiceDatabase() {
            RelyingPartyCsrResource csrResource = ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntityRepository.saveAndFlush(legalEntity);

            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            // assert that immediately returned certificate is correct.
            X509Certificate certificateActual1 =
                certService.requestAccessCertificateForRelyingParty(legalEntity.getRelyingPartyInstances().getFirst().getId(), csrResource)
                           .certificate();
            assertEquals(certificateExpected, certificateActual1);

            RelyingPartyInstance relyingPartyOut =
                instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null);

            assertNotNull(relyingPartyOut);
            assertEquals(1, relyingPartyOut.getAccessCertificates().size());

            // assert that certificate stored in DB also correct.
            X509Certificate certificateActual2 =
                relyingPartyOut.getAccessCertificates()
                               .getFirst()
                               .getCertificate();
            assertEquals(certificateExpected, certificateActual2);
        }

        @Test
        @DisplayName("then error is thrown if a CSR is registered for an unknown RP")
        public void testErrorThrownForUnknownRelyingParty() {
            RelyingPartyCsrResource csrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            UUID unknownRelyingPartyId = UUID.randomUUID();
            assertThrows(
                NotFoundException.class,
                () -> certService.requestAccessCertificateForRelyingParty(
                    unknownRelyingPartyId, csrResource)
            );
        }

        @Test
        @DisplayName("then error responses from the CA are properly handled")
        public void testErrorResponseFromCAProperlyHandled() {
            String errorResponseJson = new ObjectMapper().writeValueAsString(
                new ErrorResponseResource("invalid_request", "some error description")
            );
            MockResponse mockErrorResponse =
                new MockResponse()
                    .setResponseCode(400)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(errorResponseJson);
            mockCaServer.enqueue(mockErrorResponse);

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            UUID knownRelyingPartyId = legalEntityRepository.saveAndFlush(legalEntity).getRelyingPartyInstances().getFirst().getId();

            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            assertThrowsExactly(
                ErrorResponseException.class,
                () -> certService.requestAccessCertificateForRelyingParty(
                    knownRelyingPartyId, dummyCsrResource)
            );
        }

        @Test
        @DisplayName("then invalid certificates from the CA are properly handled")
        public void testInvalidCertificateResponseFromCAProperlyHandled() {
            String validCertificatePemStr = X509CertificateConverter.convert(CertificatesGenerator.generateX509Certificate());
            String invalidCertificatePemStr = validCertificatePemStr.toLowerCase();

            MockResponse mockInvalidSuccessResponse =
                new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-pem-file")
                    .setBody(invalidCertificatePemStr);
            mockCaServer.enqueue(mockInvalidSuccessResponse);
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();

            UUID knownRelyingPartyId = legalEntityRepository.saveAndFlush(legalEntity).getRelyingPartyInstances().getFirst().getId();

            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            assertThrowsExactly(
                CertificateConversionException.class,
                () -> certService.requestAccessCertificateForRelyingParty(
                    knownRelyingPartyId, dummyCsrResource)
            );
        }

        @Test
        @DisplayName("then null response bodies from the CA are properly handled")
        public void testNullCertificateResponseFromCAProperlyHandled() {

            MockResponse mockInvalidSuccessResponse =
                new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-pem-file");
            mockCaServer.enqueue(mockInvalidSuccessResponse);

            UUID knownRelyingPartyId =
                instanceRepository.saveAndFlush(EntityGenerator.generateRelyingParty())
                            .getId();
            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            assertThrows(
                RegisterServiceException.class,
                () -> certService.requestAccessCertificateForRelyingParty(
                    knownRelyingPartyId, dummyCsrResource)
            );
        }

        @Test
        @DisplayName("then returned cert resource has ID immediately, and this matches persisted ID")
        public void testNewCertResourceHasIdImmediatelyAndMatchesPersistedCert() {
            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();

            legalEntityRepository.saveAndFlush(legalEntity);

            RelyingPartyCsrResource csrResource = ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            enqueueMockCertificateResponse();

            RelyingPartyCertificateResource immediatelyReturnedCertResource =
                certService.requestAccessCertificateForRelyingParty(
                    legalEntity.getRelyingPartyInstances().getFirst().getId(), csrResource);

            assertNotNull(immediatelyReturnedCertResource.id());
            assertTrue(certRepository.existsById(immediatelyReturnedCertResource.id()));

            // for good measure, assert also that the immediately returned ID
            // points to the correct certificate.
            RelyingPartyCertificateResource expectedCertResource =
                certService.getCertificate(immediatelyReturnedCertResource.id(),
                                           legalEntity.getRelyingPartyInstances().getFirst().getId());
            assertEquals(expectedCertResource, immediatelyReturnedCertResource);
        }

        @Test
        @DisplayName("then CSR request is rejected for inactive RP instance")
        public void testAccessCsrRejectedForInactiveRelyingPartyInstance() {
            RelyingPartyInstance relyingPartyInstance = EntityGenerator.generateRelyingParty();
            relyingPartyInstance.setActive(false);
            instanceRepository.saveAndFlush(relyingPartyInstance);

            RelyingPartyCsrResource csrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            assertThrows(
                NotFoundException.class,
                () -> certService.requestAccessCertificateForRelyingParty(
                    relyingPartyInstance.getId(), csrResource)
            );
        }

        @Test
        @DisplayName("access certificate is default not revoked")
        public void testAccessCertificateIsDefaultNotRevoked() throws InterruptedException {
            RelyingPartyCsrResource csrResource = ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntityRepository.saveAndFlush(legalEntity);

            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            // assert that immediately returned certificate is correct.
            X509Certificate certificateActual1 =
                    certService.requestAccessCertificateForRelyingParty(legalEntity.getRelyingPartyInstances().getFirst().getId(), csrResource)
                            .certificate();
            assertEquals(certificateExpected, certificateActual1);

            RecordedRequest recordedRequest = mockCaServer.takeRequest();

            RelyingPartyInstance relyingPartyOut =
                    instanceRepository.findById(legalEntityRepository.findById(legalEntity.getId()).orElseThrow().getRelyingPartyInstances().getFirst().getId()).orElse(null);
            assertNotNull(relyingPartyOut);
            assertEquals("POST", recordedRequest.getMethod());
            assertEquals("/v1/certs/junitaccess1", recordedRequest.getPath());
            assertEquals(1, relyingPartyOut.getAccessCertificates().size());
            assertEquals(certificateActual1.getSerialNumber().toString(), relyingPartyOut.getAccessCertificates().getFirst().getSerialNo());
            assertEquals(-1, relyingPartyOut.getAccessCertificates().getFirst().getRevocationStatus());

        }
        @Test
        @DisplayName("access certificate is revoked properly")
        public void testAccessCertificateIsRevokeProperly() throws InterruptedException {
            RelyingPartyCsrResource csrResource = ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
            legalEntityRepository.saveAndFlush(legalEntity);

            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            X509Certificate certificateActual1 =
                    certService.requestAccessCertificateForRelyingParty(legalEntity.getRelyingPartyInstances().getFirst().getId(), csrResource)
                            .certificate();
            assertEquals(certificateExpected, certificateActual1);


            RelyingPartyInstance relyingPartyOut =
                    instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null);

            enqueueMockRevocationCall();

            certService.revokeAccessCertificate(relyingPartyOut.getAccessCertificates().getFirst().getId(), relyingPartyOut.getId());
            RecordedRequest recordedRequest1 = mockCaServer.takeRequest();
            RecordedRequest recordedRequest2 = mockCaServer.takeRequest();
            assertEquals("POST", recordedRequest1.getMethod());
            assertEquals("PUT", recordedRequest2.getMethod());
            assertEquals("/v1/certs/junitaccess1", recordedRequest2.getPath());


        }
    }
}

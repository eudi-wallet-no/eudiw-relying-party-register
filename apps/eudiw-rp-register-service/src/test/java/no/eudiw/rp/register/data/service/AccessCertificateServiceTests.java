package no.eudiw.rp.register.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.data.certificates.X509CertificateConverter;
import no.eudiw.rp.register.data.entity.*;
import no.eudiw.rp.register.data.repository.AccessCertificateRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.data.repository.LegalEntityRepository;
import no.eudiw.rp.register.data.service.certificates.RelyingPartyCertificateService;
import no.eudiw.rp.register.data.service.exception.ErrorResponseException;
import no.eudiw.rp.register.data.service.exception.NotFoundException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import no.eudiw.rp.register.exception.CertificateConversionException;
import no.eudiw.rp.register.api.resource.ErrorResponseResource;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.ResourceGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles("junit")
@Import(MockCaServerConfiguration.class)
@DisplayName("When using the relying party certificates service")
public class AccessCertificateServiceTests {

    @Autowired
    private RelyingPartyCertificateService certService;

    @Autowired
    private AccessCertificateRepository certRepository;

    @Autowired
    private RelyingPartyInstanceRepository instanceRepository;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    @BeforeEach
    public void clearRepositoryBeforeEachTest() {
        instanceRepository.deleteAll();
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

        @Autowired
        private MockWebServer mockCaServer;

        private X509Certificate enqueueMockCertificateResponse() throws Exception {
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
                    () -> assertEquals("/v1/certs/access", recordedRequest.getPath())
            );
        }


        @Test
        @DisplayName("then the certificate returned by CA is properly stored in the database")
        public void testCertificateFromCAProperlyStoredInRegisterServiceDatabase() throws Exception {
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
        public void testErrorThrownForUnknownRelyingParty() throws Exception {
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
        @DisplayName("then a not-found-error is thrown if a CSR is registered for a deleted RP")
        public void testErrorThrownForDeletedRelyingParty() throws Exception {
            RelyingPartyInstance relyingParty = EntityGenerator.generateRelyingParty();

            instanceRepository.saveAndFlush(relyingParty);

            RelyingPartyCsrResource csrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            assertThrows(
                NotFoundException.class,
                () -> certService.requestAccessCertificateForRelyingParty(
                    relyingParty.getId(), csrResource)
            );
        }

        @Test
        @DisplayName("then error responses from the CA are properly handled")
        public void testErrorResponseFromCAProperlyHandled() throws Exception {
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
        public void testInvalidCertificateResponseFromCAProperlyHandled() throws Exception {
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
        public void testNullCertificateResponseFromCAProperlyHandled() throws Exception {

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
        public void testNewCertResourceHasIdImmediatelyAndMatchesPersistedCert() throws Exception {
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
    }
}

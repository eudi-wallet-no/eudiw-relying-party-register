package no.eudiw.rp.register.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.data.certificates.X509CertificateConverter;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyCertificate;
import no.eudiw.rp.register.data.repository.RelyingPartyRepository;
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
@ActiveProfiles("test")
@Import(MockCaServerConfiguration.class)
@DisplayName("When using the relying party certificates service")
public class RelyingPartyCertificateServiceTests {

    @Autowired
    private RelyingPartyCertificateService certService;
    @Autowired
    private RelyingPartyRepository rpRepository;

    @BeforeEach
    public void clearRepositoryBeforeEachTest() {
        rpRepository.deleteAll();
    }

    @Nested
    @DisplayName("when reading certificates for a relying party ...")
    class GetCertificatesForRelyingPartyTests {

        @Test
        @DisplayName("then only certificates for the requested RP are returned")
        public void testGetCertificatesForRelyingParty() {
            RelyingParty relyingParty = EntityGenerator.generateRelyingPartyWithCertificates();
            RelyingParty otherRelyingParty = EntityGenerator.generateRelyingPartyWithCertificates();

            rpRepository.saveAllAndFlush(List.of(relyingParty, otherRelyingParty));

            Set<X509Certificate> certsExpected =
                relyingParty.getRelyingPartyCertificates()
                            .stream()
                            .map(RelyingPartyCertificate::getCertificate)
                            .collect(Collectors.toSet());

            Set<X509Certificate> certsActual =
                certService.getCertificatesForRelyingParty(relyingParty.getId())
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
            RelyingParty relyingParty = EntityGenerator.generateRelyingPartyWithCertificates();
            rpRepository.saveAndFlush(relyingParty);

            RelyingPartyCertificate certificate =
                relyingParty.getRelyingPartyCertificates().getFirst();

            // would throw on unknown ID(s)
            X509Certificate certificateActual =
                certService.getCertificate(certificate.getId(), relyingParty.getId())
                           .certificate();

            X509Certificate certificateExpected = certificate.getCertificate();
            assertEquals(certificateExpected, certificateActual);
        }

        @Test
        @DisplayName("then the service throws if RP ID is known but certificate ID is unknown")
        public void testErrorThrownIfRelyingPartyKnownButCertificateUnknown() {
            RelyingParty relyingParty = EntityGenerator.generateRelyingPartyWithCertificates();
            rpRepository.saveAndFlush(relyingParty);

            UUID unknownCertificateId = UUID.randomUUID();
            UUID knownRelyingPartyId = relyingParty.getId();

            assertThrows(
                NotFoundException.class,
                () -> certService.getCertificate(unknownCertificateId, knownRelyingPartyId)
            );
        }

        @Test
        @DisplayName("then the service throws if certificate ID is known but RP ID is unknown")
        public void testErrorThrownIfCertificateKnownButRelyingPartyUnknown() {
            RelyingParty relyingParty = EntityGenerator.generateRelyingPartyWithCertificates();
            rpRepository.saveAndFlush(relyingParty);

            UUID knownCertificateId =
                relyingParty.getRelyingPartyCertificates().getFirst().getId();
            UUID unknownRelyingPartyId = UUID.randomUUID();
            assertThrows(
                NotFoundException.class,
                () -> certService.getCertificate(knownCertificateId, unknownRelyingPartyId)
            );
        }

        @Test
        @DisplayName("then service throws if certificate and RP known but certificate held by different RP")
        public void testErrorThrownIfCertificateExistsForDifferentRelyingParty() {
            RelyingParty relyingParty1 = EntityGenerator.generateRelyingPartyWithCertificates();
            RelyingParty relyingParty2 = EntityGenerator.generateRelyingPartyWithCertificates();

            rpRepository.saveAllAndFlush(List.of(relyingParty1, relyingParty2));

            UUID knownCertificateId =
                relyingParty1.getRelyingPartyCertificates().getFirst().getId();
            UUID knownRelyingPartyId = relyingParty2.getId();
            assertThrows(
                NotFoundException.class,
                () -> certService.getCertificate(knownCertificateId, knownRelyingPartyId)
            );
        }

        @Test
        @DisplayName("then service throws NotFoundException when RP exists but is deleted")
        public void testCertificateNotFoundWhenRpExistsButIsDeleted() {

            RelyingParty relyingParty = EntityGenerator.generateRelyingPartyWithCertificates();
            rpRepository.saveAndFlush(relyingParty);

            RelyingPartyCertificate certificateEntity =
                relyingParty.getRelyingPartyCertificates()
                            .getFirst();

            UUID certificateId = certificateEntity.getId();
            UUID relyingPartyId = relyingParty.getId();

            X509Certificate expectedCertificate = certificateEntity.getCertificate();
            X509Certificate actualCertificate =
                certService.getCertificate(certificateId, relyingPartyId)
                           .certificate();
            assertEquals(expectedCertificate, actualCertificate);

            relyingParty.setDeleted(true);
            rpRepository.saveAndFlush(relyingParty);
            assertThrows(
                NotFoundException.class,
                () -> certService.getCertificate(certificateId, relyingPartyId)
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

            UUID registreeId =
                rpRepository.save(EntityGenerator.generateRelyingPartyNoId())
                            .getId();

            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            X509Certificate certificateActual =
                certService.requestAccessCertificateForRelyingParty(registreeId, dummyCsrResource)
                           .certificate();

            assertEquals(certificateExpected, certificateActual);
        }


        @Test
        @DisplayName("then the certificate returned by CA is properly stored in the database")
        public void testCertificateFromCAProperlyStoredInRegisterServiceDatabase()
            throws Exception {
            RelyingPartyCsrResource csrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();
            RelyingParty relyingPartyIn = EntityGenerator.generateRelyingPartyNoId();

            rpRepository.saveAndFlush(relyingPartyIn);

            X509Certificate certificateExpected = enqueueMockCertificateResponse();

            // assert that immediately returned certificate is correct.
            X509Certificate certificateActual1 =
                certService.requestAccessCertificateForRelyingParty(relyingPartyIn.getId(), csrResource)
                           .certificate();
            assertEquals(certificateExpected, certificateActual1);

            RelyingParty relyingPartyOut =
                rpRepository.findById(relyingPartyIn.getId()).orElse(null);

            assertNotNull(relyingPartyOut);
            assertEquals(1, relyingPartyOut.getRelyingPartyCertificates().size());

            // assert that certificate stored in DB also correct.
            X509Certificate certificateActual2 =
                relyingPartyOut.getRelyingPartyCertificates()
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
            RelyingParty relyingParty = EntityGenerator.generateRelyingPartyNoId();
            relyingParty.setDeleted(true);
            rpRepository.saveAndFlush(relyingParty);

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

            UUID knownRelyingPartyId =
                rpRepository.saveAndFlush(EntityGenerator.generateRelyingPartyNoId())
                            .getId();
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
            String validCertificatePemStr =
                X509CertificateConverter.convert(CertificatesGenerator.generateX509Certificate());
            String invalidCertificatePemStr = validCertificatePemStr.toLowerCase();

            MockResponse mockInvalidSuccessResponse =
                new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-pem-file")
                    .setBody(invalidCertificatePemStr);
            mockCaServer.enqueue(mockInvalidSuccessResponse);

            UUID knownRelyingPartyId =
                rpRepository.saveAndFlush(EntityGenerator.generateRelyingPartyNoId())
                            .getId();
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
                rpRepository.saveAndFlush(EntityGenerator.generateRelyingPartyNoId())
                            .getId();
            RelyingPartyCsrResource dummyCsrResource =
                ResourceGenerator.generateRegisterRelyingPartyCsrResource();

            assertThrows(
                RegisterServiceException.class,
                () -> certService.requestAccessCertificateForRelyingParty(
                    knownRelyingPartyId, dummyCsrResource)
            );
        }
    }
}

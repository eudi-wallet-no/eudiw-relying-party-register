package no.idporten.eudiw.ca.service;


import no.idporten.eudiw.ca.config.CertificateAuthorities;
import no.idporten.eudiw.ca.config.CertificateAuthority;
import no.idporten.eudiw.ca.data.Certificate;
import no.idporten.eudiw.ca.data.CertificateRepository;
import no.idporten.eudiw.ca.exception.CertificateAuthorityException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ActiveProfiles("test")
@SpringBootTest
public class CertificateAuthorityServiceTest {

    @Autowired
    private CertificateAuthorityService certificateAuthorityService;

    @Autowired
    private CertificateAuthorities certificateAuthorities;

    @MockitoBean
    private CertificateRepository certificateRepository;

    @Captor
    private ArgumentCaptor<Certificate> certificateCaptor;

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @DisplayName("When signing access certificates")
    @Nested
    class AccessCertificateTests {

        @DisplayName("then a valid certificate is created with san extensions and extended key usage for mdoc authentication")
        @Test
        void testAccessCertificate() throws Exception {
            String csr = """
                    -----BEGIN NEW CERTIFICATE REQUEST-----
                    MIIBbTCCARQCAQAwXzELMAkGA1UEBhMCbm8xDTALBgNVBAgTBFNvZ24xEjAQBgNV
                    BAcTCUxlaWthbmdlcjEPMA0GA1UEChMGRGlnZGlyMQ4wDAYDVQQLEwVFVURJVzEM
                    MAoGA1UEAxMDcnAyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELKyeEr6OlEgW
                    E0cRI3aCgzRnPu9IjoYCsPuV53/QwBe0pymYVafMPssBqiLEyuylH/AQ3Teltq66
                    L96/KVs1bqBTMFEGCSqGSIb3DQEJDjFEMEIwHQYDVR0OBBYEFFLDKogDLA5GDhgY
                    oiRDkMpjeQDNMCEGA1UdEQQaMBiCFmp1bml0LnJwMS5pZHBvcnRlbi5kZXYwCgYI
                    KoZIzj0EAwMDRwAwRAIgVIhOFcOMK0KR9MvK3a76Hgma6susPfXDJ+HfZZe50N8C
                    IF5nyI5eYXYbBBQvdAZFJStX4YgEc+7j/QV3BlIGz2HE
                    -----END NEW CERTIFICATE REQUEST-----""";
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("access");
            X509Certificate issuedCertificate = certificateAuthorityService.signCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), "991825827");
            assertAll(
                    () -> assertNotNull(issuedCertificate),
                    () -> assertTrue(issuedCertificate.getBasicConstraints() < 0),
                    () -> assertArrayEquals(new boolean[]{true, false, true, false, false, false, false, false, false}, issuedCertificate.getKeyUsage()),
                    () -> assertTrue(issuedCertificate.getSubjectAlternativeNames().iterator().next().contains("junit.rp1.idporten.dev")),
                    () -> assertEquals(intermediate.getCertificate().getSubjectX500Principal(), issuedCertificate.getIssuerX500Principal()),
                    () -> assertNotNull(issuedCertificate.getExtendedKeyUsage()),
                    () -> assertTrue(issuedCertificate.getExtendedKeyUsage().contains("1.0.18013.5.1.6")),
                    () -> assertEquals("SHA512WITHECDSA", issuedCertificate.getSigAlgName()),
                    () -> assertEquals("1.2.840.10045.4.3.4", issuedCertificate.getSigAlgOID())
            );
            issuedCertificate.verify(intermediate.getPublicKey());
            verify(certificateRepository).save(certificateCaptor.capture());
            Certificate savedCertificate = certificateCaptor.getValue();
            assertAll(
                    () -> assertEquals("access", savedCertificate.getIssuerCa()),
                    () -> assertEquals(issuedCertificate.getSerialNumber(), savedCertificate.getSerialNo()),
                    () -> assertEquals(issuedCertificate.getNotBefore().getTime(), savedCertificate.getValidFromMs()),
                    () -> assertEquals(issuedCertificate.getNotAfter().getTime(), savedCertificate.getValidUntilMs()),
                    () -> assertEquals(0, savedCertificate.getRevokedAtMs()),
                    () -> assertEquals(-1, savedCertificate.getRevocationReason())
            );
        }

        @DisplayName("then a CSR with unrecognized extensions is rejected")
        @Test
        void testUnrecognizedCSRExtensions() throws Exception {
            String csr = """
                    -----BEGIN CERTIFICATE REQUEST-----
                    MIIBkTCCATYCAQAwYzELMAkGA1UEBhMCbm8xKTAnBgNVBAsTIGV1ZGl3LXZlcmlm
                    aWVyLWRlbW8uaWRwb3J0ZW4uZGV2MSkwJwYDVQQDEyBldWRpdy12ZXJpZmllci1k
                    ZW1vLmlkcG9ydGVuLmRldjBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHKIs2py
                    gJfIIk1Z6KYl6igCERMo8SC1WAAmsPFPKdOkwp0SSIPER3BXW8BOjj2DFYV7pP1L
                    VBQLKwYCfmY7pa6gcTBvBgkqhkiG9w0BCQ4xYjBgMB0GA1UdDgQWBBSgCT60zM6D
                    kjq+jKsUVzcBhUbEbjArBgNVHREEJDAigiBldWRpdy12ZXJpZmllci1kZW1vLmlk
                    cG9ydGVuLmRldjASBgNVHRIECzAJggdiYXIuZm9vMAoGCCqGSM49BAMDA0kAMEYC
                    IQDF8LBk3ZIIITOR79QMmnkApy0Pl7R7OUay5h46CjGAXgIhAIrYLKuREXZKBFE+
                    7OFCB31ZpyqyG8YOVrifEPf8dX9D
                    -----END CERTIFICATE REQUEST-----""";
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("access");
            CertificateAuthorityException exception = assertThrows(CertificateAuthorityException.class, () -> certificateAuthorityService.signCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), "991825827"));
            assertAll(
                    () -> assertEquals("invalid_request", exception.getError()),
                    () -> assertEquals(400, exception.getHttpStatus().value()),
                    () -> assertTrue(exception.getErrorDescription().contains("contains unrecognized extension")),
                    () -> assertTrue(exception.getErrorDescription().contains("2.5.29.18"))
            );
            verifyNoInteractions(certificateRepository);
        }

        @DisplayName("then a CSR with missing required extensions is rejected")
        @Test
        void testMissingRequiredCSRExtensions() throws Exception {
            String csr = """
                    -----BEGIN NEW CERTIFICATE REQUEST-----
                    MIIBYzCCAQkCAQAwYzELMAkGA1UEBhMCbm8xKTAnBgNVBAsTIGV1ZGl3LXZlcmlm
                    aWVyLWRlbW8uaWRwb3J0ZW4uZGV2MSkwJwYDVQQDEyBldWRpdy12ZXJpZmllci1k
                    ZW1vLmlkcG9ydGVuLmRldjBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHKIs2py
                    gJfIIk1Z6KYl6igCERMo8SC1WAAmsPFPKdOkwp0SSIPER3BXW8BOjj2DFYV7pP1L
                    VBQLKwYCfmY7pa6gRDBCBgkqhkiG9w0BCQ4xNTAzMB0GA1UdDgQWBBSgCT60zM6D
                    kjq+jKsUVzcBhUbEbjASBgNVHRIECzAJggdiYXIuZm9vMAoGCCqGSM49BAMDA0gA
                    MEUCIGfooGcXWqd9+1M6j16wsNm/5B8XscHW3h+e0RpmiWADAiEA6f7olc4kLFAW
                    Cn81LRLTrsAaIRzPX4BrEGFX4J6Zvdw=
                    -----END NEW CERTIFICATE REQUEST-----""";
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("access");
            CertificateAuthorityException exception = assertThrows(CertificateAuthorityException.class, () -> certificateAuthorityService.signCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), "991825827"));
            assertAll(
                    () -> assertEquals("invalid_request", exception.getError()),
                    () -> assertEquals(400, exception.getHttpStatus().value()),
                    () -> assertTrue(exception.getErrorDescription().contains("does not contain required extension")),
                    () -> assertTrue(exception.getErrorDescription().contains("2.5.29.17"))
            );
            verifyNoInteractions(certificateRepository);
        }

   }

    @DisplayName("When signing root CA certificates")
    @Nested
    class RootCATests {

        @DisplayName("then a self signed root contains the expected extensions")
        @Test
        void testSelfSignedRootCA() throws Exception {
            CertificateAuthority certificateAuthority = certificateAuthorities.getRoot();
            PKCS10CertificationRequest csr = certificateAuthorityService.createCSR(certificateAuthority);
            X509Certificate certificate = certificateAuthorityService.signRootCertificate(certificateAuthority, csr);
            assertAll(
                    () -> assertNotNull(certificate),
                    () -> assertTrue(certificate.getBasicConstraints() > 0),
                    () -> assertArrayEquals(new boolean[]{false, false, false, false, false, true, true, false, false}, certificate.getKeyUsage()),
                    () -> assertNull(certificate.getSubjectAlternativeNames()),
                    () -> assertEquals(certificateAuthority.getCertificate().getSubjectX500Principal(), certificate.getIssuerX500Principal())
            );
            certificate.verify(certificateAuthority.getPublicKey());
            verifyNoInteractions(certificateRepository);

        }
    }

    @DisplayName("When signing intermediate CA certificates")
    @Nested
    class IntermediateCATests {

        @DisplayName("then an intermediate CA is signed by the root CA and can be used for certificate and CRL signing")
        @Test
        void testSignIntermediateCA() throws Exception {
            CertificateAuthority root = certificateAuthorities.getRoot();
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("access");
            PKCS10CertificationRequest csr = certificateAuthorityService.createCSR(intermediate);
            X509Certificate certificate = certificateAuthorityService.signIntermediateCertificate(root, csr);
            assertAll(
                    () -> assertNotNull(certificate),
                    () -> assertTrue(certificate.getBasicConstraints() > 0),
                    () -> assertArrayEquals(new boolean[]{false, false, false, false, false, true, true, false, false}, certificate.getKeyUsage()),
                    () -> assertNull(certificate.getSubjectAlternativeNames()),
                    () -> assertEquals(root.getCertificate().getSubjectX500Principal(), certificate.getIssuerX500Principal())
            );
            certificate.verify(root.getPublicKey());
            verifyNoInteractions(certificateRepository);
        }
    }

}

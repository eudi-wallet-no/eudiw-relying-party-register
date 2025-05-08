package no.idporten.eudiw.ca.service;


import no.idporten.eudiw.ca.config.CertificateAuthorities;
import no.idporten.eudiw.ca.config.CertificateAuthority;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
public class CertificateAuthorityServiceTest {

    @Autowired
    private CertificateAuthorityService certificateAuthorityService;

    @Autowired
    private CertificateAuthorities certificateAuthorities;

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
            X509Certificate certificate = certificateAuthorityService.signCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), "991825827");
            assertAll(
                    () -> assertNotNull(certificate),
                    () -> assertTrue(certificate.getBasicConstraints() < 0),
                    () -> assertArrayEquals(new boolean[]{true, false, true, false, false, false, false, false, false}, certificate.getKeyUsage()),
                    () -> assertTrue(certificate.getSubjectAlternativeNames().iterator().next().contains("junit.rp1.idporten.dev")),
                    () -> assertEquals(intermediate.getCertificate().getSubjectX500Principal(), certificate.getIssuerX500Principal()),
                    () -> assertNotNull(certificate.getExtendedKeyUsage()),
                    () -> assertTrue(certificate.getExtendedKeyUsage().contains("1.0.18013.5.1.6"))
            );
            certificate.verify(intermediate.getPublicKey());
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
        }
    }

}

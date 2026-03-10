package no.idporten.eudiw.ca.service;


import no.idporten.eudiw.ca.config.CertificateAuthorities;
import no.idporten.eudiw.ca.config.CertificateAuthority;
import no.idporten.eudiw.ca.data.Certificate;
import no.idporten.eudiw.ca.data.CertificateRepository;
import no.idporten.eudiw.ca.data.SerialNumberUtils;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.security.auth.x500.X500Principal;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
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

        @DisplayName("then a valid certificate is created with requested extensions and extended key usage for mdoc authentication")
        @Test
        void testSignRPAccessCertificate() throws Exception {
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
            X509Certificate issuedCertificate = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), new SubjectAttributes("991825827", "foo", " tfoo "));
            assertAll(
                    () -> assertNotNull(issuedCertificate),
                    () -> assertTrue(issuedCertificate.getBasicConstraints() < 0),
                    () -> assertArrayEquals(new boolean[]{true, false, true, false, false, false, false, false, false}, issuedCertificate.getKeyUsage()),
                    () -> assertTrue(issuedCertificate.getSubjectAlternativeNames().iterator().next().contains("junit.rp1.idporten.dev")),
                    () -> assertEquals(intermediate.getCertificate().getSubjectX500Principal(), issuedCertificate.getIssuerX500Principal()),
                    () -> assertNotNull(issuedCertificate.getExtendedKeyUsage()),
                    () -> assertTrue(issuedCertificate.getExtendedKeyUsage().contains("1.0.18013.5.1.6")),
                    () -> assertEquals("SHA256WITHECDSA", issuedCertificate.getSigAlgName()),
                    () -> assertEquals("1.2.840.10045.4.3.2", issuedCertificate.getSigAlgOID()),
                    () -> assertNotNull(issuedCertificate.getExtensionValue(Extension.cRLDistributionPoints.getId())),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("C=NO")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("O=foo")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("CN=tfoo")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("NTRNO-NOFOR.99182582")),
                    () -> assertEquals(100,
                            ChronoUnit.DAYS.between(
                                    LocalDate.ofInstant(issuedCertificate.getNotBefore().toInstant(), ZoneId.systemDefault()),
                                    LocalDate.ofInstant(issuedCertificate.getNotAfter().toInstant(), ZoneId.systemDefault())))
            );
            issuedCertificate.verify(intermediate.getPublicKey());
            verify(certificateRepository).save(certificateCaptor.capture());
            Certificate savedCertificate = certificateCaptor.getValue();
            assertAll(
                    () -> assertEquals("access", savedCertificate.getIssuerCa()),
                    () -> assertEquals(issuedCertificate.getSerialNumber(), SerialNumberUtils.convertFromString(savedCertificate.getSerialNo())),
                    () -> assertEquals(issuedCertificate.getNotBefore().getTime(), savedCertificate.getValidFromMs()),
                    () -> assertEquals(issuedCertificate.getNotAfter().getTime(), savedCertificate.getValidUntilMs()),
                    () -> assertEquals(0, savedCertificate.getRevokedAtMs()),
                    () -> assertEquals(-1, savedCertificate.getRevocationReason())
            );
        }

        @DisplayName("then unrecognized CSR extensions are ignored")
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
            /*
    Certificate Request decoded:
    Data:
        Attributes:
            Requested Extensions:
                X509v3 Subject Key Identifier:
                    A0:09:3E:B4:CC:CE:83:92:3A:BE:8C:AB:14:57:37:01:85:46:C4:6E
                X509v3 Subject Alternative Name:
                    DNS:eudiw-verifier-demo.idporten.dev    <-- Known
                X509v3 Issuer Alternative Name:             <-- Unknown https://www.alvestrand.no/objectid/2.5.29.18.html
                    DNS:bar.foo
             */
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("access");
            X509Certificate issuedCertificate = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), new SubjectAttributes("991825827", "foo", "tfoo"));
            assertAll(
                    () -> assertTrue(issuedCertificate.getSubjectAlternativeNames().iterator().next().contains("eudiw-verifier-demo.idporten.dev")),
                    () -> assertFalse(issuedCertificate.getCriticalExtensionOIDs().contains(Extension.issuerAlternativeName.getId())),
                    () -> assertFalse(issuedCertificate.getNonCriticalExtensionOIDs().contains(Extension.issuerAlternativeName.getId())),
                    () -> assertNull(issuedCertificate.getExtensionValue(Extension.issuerAlternativeName.getId()))
            );
        }

        @DisplayName("then not requesting known extensions is allowed")
        @Test
        void testNotRequestingCSRExtensions() throws Exception {
            String csr = """
                    -----BEGIN NEW CERTIFICATE REQUEST-----
                    MIIBRDCB6gIBADBYMRgwFgYDVQRhEw9OVFJOTy05OTE4MjU4MjcxCzAJBgNVBAYT
                    Am5vMQ8wDQYDVQQLEwZEaWdkaXIxHjAcBgNVBAMTFUVVRElXIGlzc3VlciBDQSBq
                    dW5pdDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABN2dGw4vuzpR/UX374526zoC
                    FncBNxDs9i4y33NP9RITOwMNo+hqfXiRV93ndxzUjbi8ACOo0dmF32Cq5nQCD+2g
                    MDAuBgkqhkiG9w0BCQ4xITAfMB0GA1UdDgQWBBRhT9bK8Cex2asEEZv+aIbLU8sb
                    ZjAKBggqhkjOPQQDAwNJADBGAiEA69BmlqnJUy7N5AYUr4WJbzP9lgig6ilyQC6A
                    U2Ola9ECIQDI5x5tTEVXIeCpqD+bo067mOUMicjRWwMSeGW5PGTxLg==
                    -----END NEW CERTIFICATE REQUEST-----""";
            /*
    Certificate Request decoded:
    Data:
       Attributes:
            Requested Extensions:
                X509v3 Subject Key Identifier:
                    61:4F:D6:CA:F0:27:B1:D9:AB:04:11:9B:FE:68:86:CB:53:CB:1B:66
             */
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("access");
            X509Certificate issuedCertificate = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), new SubjectAttributes("991825827", "foo", "tfoo"));
            assertAll(
                    () -> assertNull(issuedCertificate.getSubjectAlternativeNames()),
                    () -> assertNotNull(issuedCertificate.getExtensionValue(Extension.subjectKeyIdentifier.getId()))
            );
        }

    }

    @DisplayName("When signing issuer certificates")
    @Nested
    class IssuerCertificateTests {

        @DisplayName("then a valid EAA certificate is created with expected liftime")
        @Test
        void testEaaProviderCertificate() throws Exception {
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
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("eaa_provider");
            X509Certificate issuedCertificate = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), new SubjectAttributes("991825827", "foo", "tfoo"));
            assertAll(
                    () -> assertNotNull(issuedCertificate),
                    () -> assertTrue(issuedCertificate.getBasicConstraints() < 0),
                    () -> assertArrayEquals(new boolean[]{true, false, true, false, false, false, false, false, false}, issuedCertificate.getKeyUsage()),
                    () -> assertTrue(issuedCertificate.getSubjectAlternativeNames().iterator().next().contains("junit.rp1.idporten.dev")),
                    () -> assertEquals(intermediate.getCertificate().getSubjectX500Principal(), issuedCertificate.getIssuerX500Principal()),
                    () -> assertEquals("SHA256WITHECDSA", issuedCertificate.getSigAlgName()),
                    () -> assertEquals("1.2.840.10045.4.3.2", issuedCertificate.getSigAlgOID()),
                    () -> assertNotNull(issuedCertificate.getExtensionValue(Extension.cRLDistributionPoints.getId())),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("C=NO")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("O=foo")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("CN=tfoo")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("NTRNO-NOFOR.99182582")),
                    () -> assertEquals(365,
                            ChronoUnit.DAYS.between(
                                    LocalDate.ofInstant(issuedCertificate.getNotBefore().toInstant(), ZoneId.systemDefault()),
                                    LocalDate.ofInstant(issuedCertificate.getNotAfter().toInstant(), ZoneId.systemDefault())))
                    );
            issuedCertificate.verify(intermediate.getPublicKey());
            verify(certificateRepository).save(certificateCaptor.capture());
            Certificate savedCertificate = certificateCaptor.getValue();
            assertAll(
                    () -> assertEquals("eaa_provider", savedCertificate.getIssuerCa()),
                    () -> assertEquals(issuedCertificate.getSerialNumber(), SerialNumberUtils.convertFromString(savedCertificate.getSerialNo())),
                    () -> assertEquals(issuedCertificate.getNotBefore().getTime(), savedCertificate.getValidFromMs()),
                    () -> assertEquals(issuedCertificate.getNotAfter().getTime(), savedCertificate.getValidUntilMs()),
                    () -> assertEquals(0, savedCertificate.getRevokedAtMs()),
                    () -> assertEquals(-1, savedCertificate.getRevocationReason())
            );
        }

        @DisplayName("then a valid pid certificate is created with qc-statements")
        @Test
        void testPidProviderCertificate() throws Exception {
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
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("pid_provider");
            X509Certificate issuedCertificate = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), new SubjectAttributes("991825827", "foo", "tfoo"));

            assertAll(
                    () -> assertNotNull(issuedCertificate),
                    () -> assertTrue(issuedCertificate.getBasicConstraints() < 0),
                    () -> assertArrayEquals(new boolean[]{true, false, true, false, false, false, false, false, false}, issuedCertificate.getKeyUsage()),
                    () -> assertTrue(issuedCertificate.getSubjectAlternativeNames().iterator().next().contains("junit.rp1.idporten.dev")),
                    () -> assertEquals(intermediate.getCertificate().getSubjectX500Principal(), issuedCertificate.getIssuerX500Principal()),
                    () -> assertEquals("SHA256WITHECDSA", issuedCertificate.getSigAlgName()),
                    () -> assertEquals("1.2.840.10045.4.3.2", issuedCertificate.getSigAlgOID()),
                    () -> assertNotNull(issuedCertificate.getExtensionValue(Extension.cRLDistributionPoints.getId())),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("C=NO")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("O=foo")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("CN=tfoo")),
                    () -> assertTrue(issuedCertificate.getSubjectX500Principal().getName(X500Principal.RFC1779).contains("NTRNO-NOFOR.99182582")),
                    () -> assertEquals(365,
                            ChronoUnit.DAYS.between(
                                    LocalDate.ofInstant(issuedCertificate.getNotBefore().toInstant(), ZoneId.systemDefault()),
                                    LocalDate.ofInstant(issuedCertificate.getNotAfter().toInstant(), ZoneId.systemDefault()))),
                    () -> assertNotNull(issuedCertificate.getExtensionValue(Extension.qCStatements.getId())),
                    () -> assertEquals("id-etsi-qct-pid", QCStatement.getInstance(ASN1OctetString.getInstance(issuedCertificate.getExtensionValue(Extension.qCStatements.getId())).getOctets()).getStatementInfo().toString())
            );
            issuedCertificate.verify(intermediate.getPublicKey());
            verify(certificateRepository).save(certificateCaptor.capture());
            Certificate savedCertificate = certificateCaptor.getValue();
            assertAll(
                    () -> assertEquals("pid_provider", savedCertificate.getIssuerCa()),
                    () -> assertEquals(issuedCertificate.getSerialNumber(), SerialNumberUtils.convertFromString(savedCertificate.getSerialNo())),
                    () -> assertEquals(issuedCertificate.getNotBefore().getTime(), savedCertificate.getValidFromMs()),
                    () -> assertEquals(issuedCertificate.getNotAfter().getTime(), savedCertificate.getValidUntilMs()),
                    () -> assertEquals(0, savedCertificate.getRevokedAtMs()),
                    () -> assertEquals(-1, savedCertificate.getRevocationReason())
            );
        }



    }


    @DisplayName("When signing root CA certificates")
    @Nested
    class RootCATests {

        @DisplayName("then a self signed root contains the expected extensions")
        @Test
        void testSelfSignedRootCA() throws Exception {
            CertificateAuthority certificateAuthority = certificateAuthorities.findRoot("root");
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
            CertificateAuthority intermediate = certificateAuthorities.findIntermediate("access");
            CertificateAuthority root = certificateAuthorities.findRoot(intermediate.getRoot());
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

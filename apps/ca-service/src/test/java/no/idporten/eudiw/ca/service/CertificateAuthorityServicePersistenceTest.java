package no.idporten.eudiw.ca.service;


import no.idporten.eudiw.ca.config.CertificateAuthorities;
import no.idporten.eudiw.ca.config.CertificateAuthority;
import no.idporten.eudiw.ca.data.Certificate;
import no.idporten.eudiw.ca.data.CertificateRepository;
import org.apache.commons.collections.CollectionUtils;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling leaf certificates")
@ActiveProfiles("test")
@SpringBootTest
public class CertificateAuthorityServicePersistenceTest {

    @Autowired
    private CertificateAuthorityService certificateAuthorityService;

    @Autowired
    private CertificateAuthorities certificateAuthorities;

    @Autowired
    private CertificateRepository certificateRepository;

    @BeforeAll
    static void setUpBouncyCastle() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeEach
    void setUpDatabase() {
        certificateRepository.deleteAll();
    }

    @DisplayName("then certificates can be issued and revoked")
    @Transactional
    @Test
    void testCertificateLifecycle() throws Exception {
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
        X509Certificate issuedCertificate1 = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), "foo", "991825827");
        X509Certificate issuedCertificate2 = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), "bar", "991825827");
        X509Certificate issuedCertificate3 = certificateAuthorityService.signLeafCertificate(intermediate, certificateAuthorityService.decodeCsr(csr), "baz", "991825827");

        // empty crl
        X509CRL emptyCRL = certificateAuthorityService.createCRL(intermediate);
        assertTrue(CollectionUtils.isEmpty(emptyCRL.getRevokedCertificates()));

        // revoke 2 certs, expire c2
        certificateAuthorityService.revokeCertificate(intermediate, issuedCertificate1);
        Certificate c2 = certificateRepository.findByIssuerCaAndSerialNo(intermediate.getId(), issuedCertificate2.getSerialNumber());
        c2.revoke(CRLReason.keyCompromise);
        c2.setValidUntilMs(c2.getValidFromMs());
        certificateRepository.saveAndFlush(c2);
        assertEquals(3, certificateRepository.findAll().size());

        // fetch CRL
        X509CRL crl = certificateAuthorityService.createCRL(intermediate);

        assertAll(
                () -> assertNotNull(crl),
                () -> assertEquals(1, crl.getRevokedCertificates().size()),
                () -> assertNotNull(crl.getRevokedCertificate(issuedCertificate1.getSerialNumber())),
                () -> assertNull(crl.getRevokedCertificate(issuedCertificate2.getSerialNumber())),
                () -> assertNull(crl.getRevokedCertificate(issuedCertificate3.getSerialNumber())),
                () -> assertEquals(java.security.cert.CRLReason.KEY_COMPROMISE, crl.getRevokedCertificate(issuedCertificate1.getSerialNumber()).getRevocationReason())
        );
    }

}

package no.eudiw.rp.register.data.repository;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.certificates.RelyingPartyCertificate;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using IssuerCertificateRepository")
public class IssuerCertificateRepositoryTest {

    @Autowired
    private IssuerCertificateRepository certificateRepository;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;

    @Autowired
    private EntityManager entityManager;


    @Test
    @Transactional
    @DisplayName("add issuer certificate and save via issuer-repo")
    void addIssuerCertificateAndSaveViaIssuerRepo() {
        WalletRelyingParty rp = EntityGenerator.generateWalletRelyingParty();
        WalletRelyingParty saved = walletRelyingPartyRepository.save(rp);

        WalletRelyingParty savedWalletRelyingParty = walletRelyingPartyRepository.findById(saved.getId()).get();
        RelyingPartyEntitlement entitlement = EntityGenerator.instances(savedWalletRelyingParty).getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, "caId", entitlement);
        certificateRepository.save(issuerCertificate);

        entityManager.flush();
        entityManager.clear();

        IssuerCertificate retrievedIssuerCertificate = certificateRepository.findById(issuerCertificate
            .getId()).get();
        assertNotNull(retrievedIssuerCertificate);
        assertEquals(issuerCertificate, retrievedIssuerCertificate);
        assertEquals("caId", retrievedIssuerCertificate.getCaId());
        assertEquals(testCert, retrievedIssuerCertificate.getCertificate());
        assertEquals(RelyingPartyCertificate.NOT_REVOKED, retrievedIssuerCertificate.getRevocationStatus());
        assertEquals(entitlement.getId(), retrievedIssuerCertificate.getEntitlement().getId());
    }

    @Test
    @Transactional
    @DisplayName("add issuer certificate and save via rp-repo")
    void addIssuerCertificateAndSaveViaRP() {
        WalletRelyingParty rp = EntityGenerator.generateWalletRelyingParty();
        WalletRelyingParty saved = walletRelyingPartyRepository.save(rp);

        WalletRelyingParty savedWalletRelyingParty = walletRelyingPartyRepository.findById(saved.getId()).get();
        RelyingPartyEntitlement entitlement = EntityGenerator.instances(savedWalletRelyingParty).getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, "caId", entitlement);
        entitlement.setIssuerCertificates(List.of(issuerCertificate));
        walletRelyingPartyRepository.save(savedWalletRelyingParty);

        entityManager.flush();
        entityManager.clear();

        IssuerCertificate reloadedCertificate = certificateRepository.findAll().stream()
            .filter(certificate -> certificate.getSerialNo().equals(testCert.getSerialNumber().toString(10)))
            .findFirst()
            .orElseThrow();
        assertEquals("caId", reloadedCertificate.getCaId());
        assertEquals(testCert, reloadedCertificate.getCertificate());
        assertEquals(RelyingPartyCertificate.NOT_REVOKED, reloadedCertificate.getRevocationStatus());
        assertEquals(entitlement.getId(), reloadedCertificate.getEntitlement().getId());
    }

    @Test
    @Transactional
    @DisplayName("issuer certificate revocation is persisted")
    void issuerCertificateRevocationIsPersisted() {
        WalletRelyingParty rp = EntityGenerator.generateWalletRelyingParty();
        WalletRelyingParty saved = walletRelyingPartyRepository.save(rp);

        WalletRelyingParty savedWalletRelyingParty = walletRelyingPartyRepository.findById(saved.getId()).get();
        RelyingPartyEntitlement entitlement = EntityGenerator.instances(savedWalletRelyingParty).getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, "caId", entitlement);
        certificateRepository.save(issuerCertificate);

        entityManager.flush();
        entityManager.clear();

        IssuerCertificate reloadedCertificate = certificateRepository.findById(issuerCertificate.getId()).orElseThrow();
        assertEquals("caId", reloadedCertificate.getCaId());
        assertEquals(testCert, reloadedCertificate.getCertificate());
        assertEquals(RelyingPartyCertificate.NOT_REVOKED, reloadedCertificate.getRevocationStatus());
        reloadedCertificate.revoke(0);

        entityManager.flush();
        entityManager.clear();

        IssuerCertificate revokedCertificate = certificateRepository.findById(issuerCertificate.getId()).orElseThrow();
        assertEquals(0, revokedCertificate.getRevocationStatus());
    }
}

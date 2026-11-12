package no.eudiw.rp.register.data.repository;

import jakarta.persistence.EntityManager;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.certificates.RelyingPartyCertificate;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.service.RelyingPartyCertificateService;
import no.eudiw.rp.register.service.RelyingPartyService;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private RelyingPartyCertificateService certificateService;

    @Autowired
    private RelyingPartyService relyingPartyService;

    @Autowired
    private PlatformTransactionManager transactionManager;


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
        entitlement.addIssuerCertificate(issuerCertificate);
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
    @DisplayName("Should keep both issuer certificates with their owner when the role reference changes")
    void shouldKeepIssuerCertificateHistoryWithItsInstanceAfterRoleReferenceChanges() {
        var ownerParty = walletRelyingPartyRepository.save(EntityGenerator.generateWalletRelyingParty());
        var otherParty = walletRelyingPartyRepository.save(EntityGenerator.generateWalletRelyingParty());
        var owner = EntityGenerator.instances(ownerParty).getFirst();
        var other = EntityGenerator.instances(otherParty).getFirst();
        var entitlement = new RelyingPartyEntitlement("urn:test:certificate-owner");
        owner.setRelyingPartyEntitlements(List.of(entitlement));
        var first = new IssuerCertificate(CertificatesGenerator.generateX509Certificate(), "ca", entitlement);
        var rotated = new IssuerCertificate(CertificatesGenerator.generateX509Certificate(), "ca", entitlement);
        entitlement.addIssuerCertificate(first);
        entitlement.addIssuerCertificate(rotated);
        entityManager.flush();

        entityManager.createNativeQuery("""
            UPDATE relying_party_entitlement SET relying_party_instance_id = :instanceId WHERE id = :entitlementId
            """)
            .setParameter("instanceId", other.getId().toString())
            .setParameter("entitlementId", entitlement.getId().toString())
            .executeUpdate();
        entityManager.clear();

        for (var certificate : List.of(first, rotated)) {
            var stored = certificateRepository.findByIdAndRelyingPartyInstanceId(certificate.getId(), owner.getId())
                .orElseThrow();
            assertEquals(owner.getId(), stored.getRelyingPartyInstance().getId());
            assertEquals(other.getId(), stored.getEntitlement().getRelyingPartyInstance().getId());
            assertTrue(certificateRepository.findByIdAndRelyingPartyInstanceId(certificate.getId(), other.getId()).isEmpty());
            assertEquals(certificate.getId(),
                certificateService.getIssuerCertificate(certificate.getId(), owner.getId()).getId());
            assertThrows(NotFoundException.class, () ->
                certificateService.getIssuerCertificate(certificate.getId(), other.getId()));
        }
        var storedOwner = entityManager.find(RelyingPartyInstance.class, owner.getId());
        assertEquals(List.of(first.getId(), rotated.getId()).stream().sorted().toList(),
            storedOwner.getIssuerCertificates().stream().map(IssuerCertificate::getId).sorted().toList());
        assertTrue(entityManager.find(RelyingPartyInstance.class, other.getId()).getIssuerCertificates().isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("Should keep the issuer certificate owner when an owner change and revocation are saved")
    void shouldKeepIssuerCertificateOwnerWhenOwnerChangeAndRevocationAreSaved() {
        var ownerParty = walletRelyingPartyRepository.save(EntityGenerator.generateWalletRelyingParty());
        var otherParty = walletRelyingPartyRepository.save(EntityGenerator.generateWalletRelyingParty());
        var owner = EntityGenerator.instances(ownerParty).getFirst();
        var other = EntityGenerator.instances(otherParty).getFirst();
        var certificate = certificateRepository.save(new IssuerCertificate(
            CertificatesGenerator.generateX509Certificate(), "ca", owner.getRelyingPartyEntitlements().getFirst()));
        entityManager.flush();
        entityManager.clear();

        var stored = certificateRepository.findById(certificate.getId()).orElseThrow();
        ReflectionTestUtils.setField(stored, "relyingPartyInstance",
            entityManager.find(RelyingPartyInstance.class, other.getId()));
        stored.revoke(0);
        entityManager.flush();
        entityManager.clear();

        var reloaded = certificateRepository.findByIdAndRelyingPartyInstanceId(certificate.getId(), owner.getId())
            .orElseThrow();
        assertEquals(owner.getId(), reloaded.getRelyingPartyInstance().getId());
        assertEquals(0, reloaded.getRevocationStatus());
        assertTrue(certificateRepository.findByIdAndRelyingPartyInstanceId(certificate.getId(), other.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should keep issuer certificates when role deletion is rejected and delete them with their instance")
    void shouldKeepIssuerCertificatesWhenRoleDeletionIsRejectedAndDeleteThemWithTheirInstance() {
        var transaction = new TransactionTemplate(transactionManager);
        var party = EntityGenerator.generateWalletRelyingParty();
        var instance = EntityGenerator.instances(party).getFirst();
        var entitlement = instance.getRelyingPartyEntitlements().getFirst();
        var certificate = new IssuerCertificate(CertificatesGenerator.generateX509Certificate(), "ca", entitlement);
        certificate.revoke(0);
        entitlement.addIssuerCertificate(certificate);
        transaction.executeWithoutResult(status -> walletRelyingPartyRepository.saveAndFlush(party));

        try {
            assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
                var storedInstance = entityManager.find(RelyingPartyInstance.class, instance.getId());
                storedInstance.getRelyingPartyEntitlements()
                    .removeIf(role -> role.getId().equals(entitlement.getId()));
                entityManager.flush();
            }));
            transaction.executeWithoutResult(status -> {
                var stored = certificateRepository.findById(certificate.getId()).orElseThrow();
                assertEquals(entitlement.getId(), stored.getEntitlement().getId());
                assertEquals(instance.getId(), stored.getRelyingPartyInstance().getId());
                assertEquals(0, stored.getRevocationStatus());
            });

            relyingPartyService.deleteRelyingParty(instance.getId());

            transaction.executeWithoutResult(status -> {
                assertFalse(certificateRepository.existsById(certificate.getId()));
                assertNull(entityManager.find(RelyingPartyEntitlement.class, entitlement.getId()));
                assertNull(entityManager.find(RelyingPartyInstance.class, instance.getId()));
            });
        } finally {
            transaction.executeWithoutResult(status -> walletRelyingPartyRepository.deleteById(party.getId()));
        }
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

package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
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

import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using IssuerCertificateRepository")
public class IssuerCertificateRepositoryTest {

    @Autowired
    private IssuerCertificateRepository issuerCertificateRepository;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingPartyRepository;


    @Test
    @DisplayName("add issuer certificate and save via issuer-repo")
    void addIssuerCertificateAndSaveViaIssuerRepo() {
        WalletRelyingParty rp = EntityGenerator.generateWalletRelyingParty();
        WalletRelyingParty saved = walletRelyingPartyRepository.save(rp);

        WalletRelyingParty savedWalletRelyingParty = walletRelyingPartyRepository.findById(saved.getId()).get();
        RelyingPartyEntitlement entitlement = EntityGenerator.instances(savedWalletRelyingParty).getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, "caId", entitlement);
        issuerCertificateRepository.save(issuerCertificate);

        IssuerCertificate retrievedIssuerCertificate = issuerCertificateRepository.findById(issuerCertificate
            .getId()).get();
        assertNotNull(retrievedIssuerCertificate);
        assertEquals(issuerCertificate, retrievedIssuerCertificate);
    }

    @Test
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

        WalletRelyingParty finalWalletRelyingParty = walletRelyingPartyRepository.findById(rp.getId()).get();
        RelyingPartyEntitlement finalEntitlement = EntityGenerator.instances(finalWalletRelyingParty).getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        assertEquals(entitlement.getIssuerCertificates().getFirst().getCertificate(), finalEntitlement.getIssuerCertificates().getFirst().getCertificate());
    }

    @Test
    @DisplayName("rp repo issuer certificate revocation test")
    void rpRepoIssuerCertificateRevocationTest() {
        WalletRelyingParty rp = EntityGenerator.generateWalletRelyingParty();
        WalletRelyingParty saved = walletRelyingPartyRepository.save(rp);

        WalletRelyingParty savedWalletRelyingParty = walletRelyingPartyRepository.findById(saved.getId()).get();
        RelyingPartyEntitlement entitlement = EntityGenerator.instances(savedWalletRelyingParty).getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, "caId", entitlement);
        entitlement.setIssuerCertificates(List.of(issuerCertificate));
        walletRelyingPartyRepository.save(savedWalletRelyingParty);

        WalletRelyingParty finalWalletRelyingParty = walletRelyingPartyRepository.findById(rp.getId()).get();
        RelyingPartyEntitlement finalEntitlement = EntityGenerator.instances(finalWalletRelyingParty).getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        assertEquals(entitlement.getIssuerCertificates().getFirst().getCertificate(), finalEntitlement.getIssuerCertificates().getFirst().getCertificate());
        EntityGenerator.instances(finalWalletRelyingParty).getFirst().getIssuerCertificates().getFirst().revoke(0);

        walletRelyingPartyRepository.save(finalWalletRelyingParty);
        assertEquals(0, EntityGenerator.instances(walletRelyingPartyRepository.findById(finalWalletRelyingParty.getId()).get()).getFirst().getIssuerCertificates().getFirst().getRevocationStatus());
    }
}

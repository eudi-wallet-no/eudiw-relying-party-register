package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.certificates.IssuerCertificate;
import no.eudiw.rp.register.data.entity.LegalEntity;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEntitlement;
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
    private LegalEntityRepository legalEntityRepository;


    @Test
    @DisplayName("add issuer certificate and save via issuer-repo")
    void addIssuerCertificateAndSaveViaIssuerRepo() {
        LegalEntity rp = EntityGenerator.generateLegalEntity();
        LegalEntity saved = legalEntityRepository.save(rp);

        LegalEntity savedLegalEntity = legalEntityRepository.findById(saved.getId()).get();
        RelyingPartyEntitlement entitlement = savedLegalEntity.getRelyingPartyInstances().getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, entitlement);
        issuerCertificateRepository.save(issuerCertificate);

        IssuerCertificate retrievedIssuerCertificate = issuerCertificateRepository.findById(issuerCertificate
            .getId()).get();
        assertNotNull(retrievedIssuerCertificate);
        assertEquals(issuerCertificate, retrievedIssuerCertificate);
    }

    @Test
    @DisplayName("add issuer certificate and save via rp-repo")
    void addIssuerCertificateAndSaveViaRP() {
        LegalEntity rp = EntityGenerator.generateLegalEntity();
        LegalEntity saved = legalEntityRepository.save(rp);

        LegalEntity savedLegalEntity = legalEntityRepository.findById(saved.getId()).get();
        RelyingPartyEntitlement entitlement = savedLegalEntity.getRelyingPartyInstances().getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, entitlement);
        entitlement.setIssuerCertificates(List.of(issuerCertificate));
        legalEntityRepository.save(savedLegalEntity);

        LegalEntity finalLegalEntity = legalEntityRepository.findById(rp.getId()).get();
        RelyingPartyEntitlement finalEntitlement = finalLegalEntity.getRelyingPartyInstances().getFirst().getRelyingPartyEntitlements().stream().findFirst().get();
        assertEquals(entitlement.getIssuerCertificates().getFirst().getCertificate(), finalEntitlement.getIssuerCertificates().getFirst().getCertificate());
    }
}

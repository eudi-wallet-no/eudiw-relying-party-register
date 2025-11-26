package no.eudiw.rp.register.data.repository;

import jakarta.annotation.Resource;
import no.eudiw.rp.register.data.entity.IssuerCertificate;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.testdata.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using IssuerCertificateRepository")
public class IssuerCertificateRepositoryTest {

    @Resource
    private IssuerCertificateRepository issuerCertificateRepository;

    @Resource
    private RelyingPartyRepository relyingPartyRepository;


    @Test
    @DisplayName("add issuer certificate and save via issuer-repo")
    void addIssuerCertificateAndSaveViaIssuerRepo() throws Exception {
        RelyingParty rp = EntityGenerator.generateRelyingPartyNoId();
        relyingPartyRepository.save(rp);

        RelyingParty savedRelyingParty = relyingPartyRepository.findById(rp.getId()).get();
        RelyingPartyEntitlement entitlement = savedRelyingParty.getRelyingPartyEntitlements().stream().findFirst().get();
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
    void addIssuerCertificateAndSaveViaRP() throws Exception {
        RelyingParty rp = EntityGenerator.generateRelyingPartyNoId();
        relyingPartyRepository.save(rp);

        RelyingParty savedRelyingParty = relyingPartyRepository.findById(rp.getId()).get();
        RelyingPartyEntitlement entitlement = savedRelyingParty.getRelyingPartyEntitlements().stream().findFirst().get();
        X509Certificate testCert = CertificatesGenerator.generateX509Certificate();
        IssuerCertificate issuerCertificate = new IssuerCertificate(testCert, entitlement);
        entitlement.setIssuerCertificates(List.of(issuerCertificate));
        relyingPartyRepository.save(savedRelyingParty);

        RelyingParty finalRelyingParty = relyingPartyRepository.findById(rp.getId()).get();
        RelyingPartyEntitlement finalEntitlement = finalRelyingParty.getRelyingPartyEntitlements().stream().findFirst().get();
        assertEquals(entitlement.getIssuerCertificates().getFirst().getCertificate(), finalEntitlement.getIssuerCertificates().getFirst().getCertificate());
    }
}

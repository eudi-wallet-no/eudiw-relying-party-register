package no.eudiw.rp.register.data.repository;

import no.eudiw.rp.register.data.entity.*;
import no.eudiw.rp.register.data.entity.certificates.AccessCertificate;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using RelyingPartyCertificates ...")
public class AccessCertificateTests {

    @Autowired
    private RelyingPartyInstanceRepository instanceRepository;

    @Autowired
    private LegalEntityRepository rpRepository;

    @AfterEach
    void clearRepositoryAfterTests() {
        instanceRepository.deleteAll();
    }

    @Nested
    @DisplayName("When creating and adding certificates ...")
    class AddAccessCertificateTests {

        @DisplayName("then relying parties can have 0 or more certificates")
        @Nested
        class MultiplicityTests {
            @Test
            @DisplayName("then creation of RP with zero certificates is successful")
            public void testNoCertificates() {
                LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
                assertTrue(legalEntity.getRelyingPartyInstances().getFirst().getAccessCertificates().isEmpty());
                rpRepository.saveAndFlush(legalEntity);

                RelyingPartyInstance rpOut = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null);

                assertNotNull(rpOut);
                assertAll(
                    () -> assertNotNull(rpOut.getAccessCertificates()),
                    () -> assertTrue(rpOut.getAccessCertificates().isEmpty())
                );
            }

            @Test
            @DisplayName("then creation of RP with one certificate is successful")
            public void testOneCertificate() {
                LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
                AccessCertificate certIn = EntityGenerator.generateCertificate();
                legalEntity.getRelyingPartyInstances().getFirst().setAccessCertificates(List.of(certIn));
                assertEquals(1,  legalEntity.getRelyingPartyInstances().getFirst().getAccessCertificates().size());
                rpRepository.saveAndFlush(legalEntity);

                RelyingPartyInstance rpOut = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null);
                assertNotNull(rpOut);

                AccessCertificate certOut =
                    rpOut.getAccessCertificates().getFirst();
                assertNotNull(certOut);

                assertAll(
                    () -> assertEquals(1, rpOut.getAccessCertificates().size()),
                    () -> assertEquals(certIn.getCertificate(), certOut.getCertificate()),
                    () -> assertEquals(certIn.getSubjectDn(), certOut.getSubjectDn()),
                    () -> assertEquals(certIn.getSerialNo(), certOut.getSerialNo())
                );
            }

            @Test
            @DisplayName("then creation of RP with multiple certificates is successful")
            public void testMultipleCertificates() {
                LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
                RelyingPartyInstance relyingPartyInstance = new RelyingPartyInstance(
                    "name",
                    List.of(),
                    List.of(),
                    List.of(EntityGenerator.generateCertificate(), EntityGenerator.generateCertificate(), EntityGenerator.generateCertificate())
                );
                legalEntity.setRelyingPartyInstances(List.of(relyingPartyInstance));
                assertTrue(relyingPartyInstance.getAccessCertificates().size() > 1);
                rpRepository.saveAndFlush(legalEntity);

                RelyingPartyInstance rpOut = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null);
                assertNotNull(rpOut);

                // NOTE: use Set in order to check equality, since the
                // repo returns a PersistentBag which is tedious to work with.
                // OK since certificates are assumed to be unique.
                Set<AccessCertificate> certsExpected =
                    new HashSet<>(relyingPartyInstance.getAccessCertificates());
                Set<AccessCertificate> certsActual =
                    new HashSet<>(rpOut.getAccessCertificates());

                assertEquals(certsExpected, certsActual);
            }
        }

        @Nested
        @DisplayName("then various properties are respected during creation")
        class IntegrityTests {
            @Test
            @DisplayName("then auxiliary fields are correctly extracted from the certificate")
            public void testAuxiliaryFieldsProperlyStoredInEntity() {
                LegalEntity legalEntity = new LegalEntity("name", "orgno", true, new ArrayList<>());
                RelyingPartyInstance relyingPartyInstance = new RelyingPartyInstance(
                    "name",
                    List.of(),
                    List.of(),
                    List.of(EntityGenerator.generateCertificate())
                );
                legalEntity.setRelyingPartyInstances(List.of(relyingPartyInstance));
                rpRepository.saveAndFlush(legalEntity);

                RelyingPartyInstance rpOut = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null);
                assertNotNull(rpOut);

                AccessCertificate certEntity =
                    rpOut.getAccessCertificates().getFirst();

                X509Certificate actualCert = certEntity.getCertificate();

                assertAll(
                        () -> assertEquals(actualCert.getSerialNumber(), new BigInteger(certEntity.getSerialNo())),
                        () -> assertEquals(actualCert.getSubjectX500Principal().getName(), certEntity.getSubjectDn()),
                        () -> assertEquals(actualCert.getNotBefore().toInstant().toEpochMilli(), certEntity.getValidFromMs()),
                        () -> assertEquals(actualCert.getNotAfter().toInstant().toEpochMilli(), certEntity.getValidUntilMs())
                );
            }
        }
    }

    @Test
    @DisplayName("RP with multiple certificates created and revocation is successfull")
    public void testMultipleCertificatesWithRevocation() {
        LegalEntity legalEntity = EntityGenerator.generateLegalEntity();
        RelyingPartyInstance relyingPartyInstance = new RelyingPartyInstance(
                "name",
                List.of(),
                List.of(),
                List.of(EntityGenerator.generateCertificate(), EntityGenerator.generateCertificate(), EntityGenerator.generateCertificate())
        );
        legalEntity.setRelyingPartyInstances(List.of(relyingPartyInstance));
        assertTrue(relyingPartyInstance.getAccessCertificates().size() > 1);
        rpRepository.saveAndFlush(legalEntity);

        RelyingPartyInstance rpOut = instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null);
        assertNotNull(rpOut);

        // NOTE: use Set in order to check equality, since the
        // repo returns a PersistentBag which is tedious to work with.
        // OK since certificates are assumed to be unique.
        Set<AccessCertificate> certsExpected =
                new HashSet<>(relyingPartyInstance.getAccessCertificates());
        Set<AccessCertificate> certsActual =
                new HashSet<>(rpOut.getAccessCertificates());

        assertEquals(certsExpected, certsActual);

        rpOut.getAccessCertificates().getFirst().revoke(0);
        instanceRepository.saveAndFlush(rpOut);
        assertEquals(0, instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null).getAccessCertificates().getFirst().getRevocationStatus());
        assertEquals(-1, instanceRepository.findById(legalEntity.getRelyingPartyInstances().getFirst().getId()).orElse(null).getAccessCertificates().get(1).getRevocationStatus());
    }
}

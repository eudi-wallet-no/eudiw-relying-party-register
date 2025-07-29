package no.eudiw.rp.register.data.repository;

import jakarta.annotation.Resource;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyCertificate;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;

import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("When using RelyingPartyCertificates ...")
public class RelyingPartyCertificateTests {

    @Resource
    private RelyingPartyRepository rpRepository;

    @AfterEach
    void clearRepositoryAfterTests() {
        rpRepository.deleteAll();
    }

    @Nested
    @DisplayName("When creating and adding certificates ...")
    class AddRelyingPartyCertificateTests {

        @DisplayName("then relying parties can have 0 or more certificates")
        @Nested
        class MultiplicityTests {
            @Test
            @DisplayName("then creation of RP with zero certificates is successful")
            public void testNoCertificates() {
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyNoId();
                assertTrue(rpIn.getRelyingPartyCertificates().isEmpty());
                rpRepository.save(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);

                assertNotNull(rpOut);
                assertAll(
                    () -> assertNotNull(rpOut.getRelyingPartyCertificates()),
                    () -> assertTrue(rpOut.getRelyingPartyCertificates().isEmpty())
                );
            }


            @Test
            @DisplayName("then creation of RP with one certificate is successful")
            public void testOneCertificate() {
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyNoId();
                RelyingPartyCertificate certIn =
                    EntityGenerator.generateCertificate();
                rpIn.setRelyingPartyCertificates(List.of(certIn));
                assertEquals(1, rpIn.getRelyingPartyCertificates().size());

                rpRepository.save(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);
                assertNotNull(rpOut);

                RelyingPartyCertificate certOut =
                    rpOut.getRelyingPartyCertificates().getFirst();
                assertNotNull(certOut);

                assertAll(
                    () -> assertEquals(1, rpOut.getRelyingPartyCertificates().size()),
                    () -> assertEquals(certIn.getCertificate(), certOut.getCertificate()),
                    () -> assertEquals(certIn.getSubjectDn(), certOut.getSubjectDn()),
                    () -> assertEquals(certIn.getSerialNo(), certOut.getSerialNo())
                );
            }

            @Test
            @DisplayName("then creation of RP with multiple certificates is successful")
            public void testMultipleCertificates() {
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyWithCertificates();
                assertTrue(rpIn.getRelyingPartyCertificates().size() > 1);

                rpRepository.save(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);
                assertNotNull(rpOut);

                // NOTE: use Set in order to check equality, since the
                // repo returns a PersistentBag which is tedious to work with.
                // OK since certificates are assumed to be unique.
                Set<RelyingPartyCertificate> certsExpected =
                    new HashSet<>(rpIn.getRelyingPartyCertificates());
                Set<RelyingPartyCertificate> certsActual =
                    new HashSet<>(rpOut.getRelyingPartyCertificates());

                assertEquals(certsExpected, certsActual);
            }
        }

        @Nested
        @DisplayName("then various properties are respected during creation")
        class IntegrityTests {
            @Test
            @DisplayName("then auxiliary fields are correctly extracted from the certificate")
            public void testAuxiliaryFieldsProperlyStoredInEntity() {
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyNoId();
                rpIn.addRelyingPartyCertificate(EntityGenerator.generateCertificate());
                rpRepository.saveAndFlush(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);
                assertNotNull(rpOut);

                RelyingPartyCertificate certEntity =
                    rpOut.getRelyingPartyCertificates().getFirst();

                X509Certificate actualCert = certEntity.getCertificate();

                assertAll(
                    () -> assertEquals(actualCert.getSerialNumber(), certEntity.getSerialNo()),
                    () -> assertEquals(actualCert.getSubjectX500Principal().getName(), certEntity.getSubjectDn()),
                    () -> assertEquals(actualCert.getSerialNumber(), certEntity.getSerialNo()),
                    () -> assertEquals(actualCert.getNotBefore().toInstant().toEpochMilli(), certEntity.getValidFromMs()),
                    () -> assertEquals(actualCert.getNotAfter().toInstant().toEpochMilli(), certEntity.getValidUntilMs())
                );
            }

            @Test
            @DisplayName("then adding the same certificate multiple times gives an error")
            public void testDuplicateCertificateIsRejected() {
                RelyingParty rpIn1 = EntityGenerator.generateRelyingPartyNoId();
                RelyingParty rpIn2 = EntityGenerator.generateRelyingPartyNoId();

                RelyingPartyCertificate cert = EntityGenerator.generateCertificate();

                rpIn1.setRelyingPartyCertificates(List.of(cert));
                rpRepository.save(rpIn1);

                rpIn2.setRelyingPartyCertificates(List.of(cert));
                assertThrows(InvalidDataAccessApiUsageException.class,
                             () -> rpRepository.saveAndFlush(rpIn2)
                );
            }
        }
    }
}

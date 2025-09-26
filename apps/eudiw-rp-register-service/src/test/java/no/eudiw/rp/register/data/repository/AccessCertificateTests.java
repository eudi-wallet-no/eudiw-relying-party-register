package no.eudiw.rp.register.data.repository;

import jakarta.annotation.Resource;
import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.AccessCertificate;
import no.eudiw.rp.register.testdata.EntityGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("When using RelyingPartyCertificates ...")
public class AccessCertificateTests {

    @Resource
    private RelyingPartyRepository rpRepository;

    @AfterEach
    void clearRepositoryAfterTests() {
        rpRepository.deleteAll();
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
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyNoId();
                assertTrue(rpIn.getAccessCertificates().isEmpty());
                rpRepository.save(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);

                assertNotNull(rpOut);
                assertAll(
                    () -> assertNotNull(rpOut.getAccessCertificates()),
                    () -> assertTrue(rpOut.getAccessCertificates().isEmpty())
                );
            }


            @Test
            @DisplayName("then creation of RP with one certificate is successful")
            public void testOneCertificate() {
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyNoId();
                AccessCertificate certIn =
                    EntityGenerator.generateCertificate();
                rpIn.setAccessCertificates(List.of(certIn));
                assertEquals(1, rpIn.getAccessCertificates().size());

                rpRepository.save(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);
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
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyWithCertificates();
                assertTrue(rpIn.getAccessCertificates().size() > 1);

                rpRepository.save(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);
                assertNotNull(rpOut);

                // NOTE: use Set in order to check equality, since the
                // repo returns a PersistentBag which is tedious to work with.
                // OK since certificates are assumed to be unique.
                Set<AccessCertificate> certsExpected =
                    new HashSet<>(rpIn.getAccessCertificates());
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
                RelyingParty rpIn = EntityGenerator.generateRelyingPartyNoId();
                rpIn.addRelyingPartyCertificate(EntityGenerator.generateCertificate());
                rpRepository.saveAndFlush(rpIn);

                RelyingParty rpOut = rpRepository.findById(rpIn.getId()).orElse(null);
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

            @Test
            @DisplayName("then adding the same certificate multiple times gives an error")
            public void testDuplicateCertificateIsRejected() {
                RelyingParty rpIn1 = EntityGenerator.generateRelyingPartyNoId();
                RelyingParty rpIn2 = EntityGenerator.generateRelyingPartyNoId();

                AccessCertificate cert = EntityGenerator.generateCertificate();

                rpIn1.setAccessCertificates(List.of(cert));
                rpRepository.save(rpIn1);

                rpIn2.setAccessCertificates(List.of(cert));
                assertThrows(InvalidDataAccessApiUsageException.class,
                             () -> rpRepository.saveAndFlush(rpIn2)
                );
            }
        }
    }
}

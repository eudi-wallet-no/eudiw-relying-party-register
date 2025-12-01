package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.data.entity.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class EntityGenerator extends TestDataGenerator {

    public static List<LegalEntity> generateRelyingParties(int n) {
        Collection<LegalEntity> distinctLegalEntities =
            generateListBy(n, EntityGenerator::generateRelyingParty)
                   .stream()
                   .collect(Collectors.toMap(LegalEntity::getOrgno, le -> le))
                   .values();
        return new ArrayList<>(distinctLegalEntities);
    }

    public static RelyingPartyInstance generateRelyingPartyNoId() {
        return new RelyingPartyInstance(
            generateName(),
            List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider"), new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")),
            generateListBy(EntityGenerator::generateEaa),
            // NOTE: certificates are very expensive to generate, so no
            // certificates by default.
            new ArrayList<>());
    }

    public static LegalEntity generateRelyingParty() {
        LegalEntity legalEntity = generateRelyingPartyWithoutInstance();

        legalEntity.addRelyingPartyInstance(generateRelyingPartyNoId(legalEntity));
        return legalEntity;
    }

    public static LegalEntity generateRelyingPartyWithoutInstance() {
        return new LegalEntity(
            generateName(),
            generateValidOrgno(),
            true,
            new ArrayList<>()
        );
    }

    public static RelyingPartyInstance generateRelyingPartyNoId(LegalEntity rp) {
        return new RelyingPartyInstance(
            generateName(),
            List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider"), new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")),
            generateListBy(EntityGenerator::generateEaa),
            // NOTE: certificates are very expensive to generate, so no
            // certificates by default.
            new ArrayList<>(),
            rp
        );
    }

    public static LegalEntity generateRelyingParty(RelyingPartyInstance instance) {
        return new LegalEntity(
            generateName(),
            generateValidOrgno(),
            true,
            List.of(instance)
        );
    }

    public static RelyingPartyInstance generateRelyingPartyWithCertificates() {
        RelyingPartyInstance relyingParty = generateRelyingPartyNoId();
        relyingParty.setAccessCertificates(EntityGenerator.generateCertificates());
        return relyingParty;
    }

    public static RelyingPartyEaa generateEaa() {
        return new RelyingPartyEaa("namespace-" + generateName(),
                                   "intent-" + generateName());
    }
    public static AccessCertificate generateCertificate() {
        try {
            return new AccessCertificate(
                CertificatesGenerator.generateX509Certificate(), null);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to generate RelyingPartyCertificate", e);
        }
    }
    public static List<AccessCertificate> generateCertificates() {
        return generateListBy(rng.nextInt(2, 4), EntityGenerator::generateCertificate);
    }
}

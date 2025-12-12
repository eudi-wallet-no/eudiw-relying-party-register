package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.data.entity.*;
import no.eudiw.rp.register.data.entity.certificates.AccessCertificate;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityGenerator extends TestDataGenerator {

    public static List<LegalEntity> generateLegalEntities(int numLegalEntities) {
        Map<String, LegalEntity> distinctLegalEntities = new HashMap<>();
        while (distinctLegalEntities.size() < numLegalEntities) {
            LegalEntity le = EntityGenerator.generateLegalEntity();
            distinctLegalEntities.put(le.getOrgno(), le);
        }
        return new ArrayList<>(distinctLegalEntities.values());
    }

    public static LegalEntity generateLegalEntity() {
        return generateLegalEntity(rng.nextInt(1, 6));
    }

    public static LegalEntity generateLegalEntity(int numInstances) {
        return new LegalEntity(
            generateName(),
            generateValidOrgno(),
            true,
            generateListBy(numInstances, EntityGenerator::generateRelyingParty));
    }

    public static RelyingPartyInstance generateRelyingParty() {
        return new RelyingPartyInstance(
            generateName(),
            List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider"), new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")),
            generateListBy(EntityGenerator::generateEaa),
            // NOTE: certificates are very expensive to generate, so no
            // certificates by default.
            new ArrayList<>(),
            null
        );
    }

    public static RelyingPartyInstance generateRelyingPartyWithLegalEntity() {
        RelyingPartyInstance relyingPartyInstance = generateRelyingParty();
        LegalEntity legalEntityWithZeroInstances = generateLegalEntity(0);
        relyingPartyInstance.setLegalEntity(legalEntityWithZeroInstances);
        return relyingPartyInstance;
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
}

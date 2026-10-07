package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.domain.LegalEntity;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

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
            generateListBy(numInstances, EntityGenerator::generateRelyingPartyWithoutLegalEntity));
    }

    public static List<RelyingPartyEntitlement> sampleRelyingPartyEntitlements() {
        return sampleEntitlements(rng.nextInt(1, 4))
                   .stream()
                   .map(entitlementValue -> {
                       boolean hasIssuerUrl = rng.nextFloat() >= 0.3;
                       String issuerUrl = hasIssuerUrl ? generateIssuerUrl() : null;
                       return new RelyingPartyEntitlement(entitlementValue, issuerUrl);
                   })
                   .toList();
    }

    public static RelyingPartyInstance generateRelyingPartyWithoutLegalEntity() {
        return new RelyingPartyInstance(
            generateName(),
            sampleRelyingPartyEntitlements(),
            generateListBy(EntityGenerator::generateEaa),
            // NOTE: certificates are very expensive to generate, so no
            // certificates by default.
            new ArrayList<>()
        );
    }

    public static RelyingPartyInstance generateRelyingParty() {
        RelyingPartyInstance relyingPartyInstance = generateRelyingPartyWithoutLegalEntity();
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
            return new AccessCertificate("access", CertificatesGenerator.generateX509Certificate(), null);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to generate RelyingPartyCertificate", e);
        }
    }
}

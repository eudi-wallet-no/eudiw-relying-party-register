package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyAccessCertificate;
import no.eudiw.rp.register.data.entity.RelyingPartyEaa;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;

import java.util.ArrayList;
import java.util.List;

public class EntityGenerator extends TestDataGenerator {

    public static List<RelyingParty> generateRelyingParties(int n) {
        return generateListBy(n, EntityGenerator::generateRelyingPartyNoId);
    }

    public static RelyingParty generateRelyingPartyNoId() {
        return new RelyingParty(
            generateName(),
            generateValidOrgno(),
            generatePublicSector(),
            generateListBy(EntityGenerator::generateEntitlement),
            generateListBy(EntityGenerator::generateEaa),
            // NOTE: certificates are very expensive to generate, so no
            // certificates by default.
            new ArrayList<>());
    }

    public static RelyingParty generateRelyingPartyWithCertificates() {
        RelyingParty relyingParty = generateRelyingPartyNoId();
        relyingParty.setRelyingPartyAccessCertificates(EntityGenerator.generateAccessCertificates());
        return relyingParty;
    }

    public static RelyingPartyEntitlement generateEntitlement() {
        return new RelyingPartyEntitlement("ent-" + generateName());
    }
    public static RelyingPartyEaa generateEaa() {
        return new RelyingPartyEaa("namespace-" + generateName(),
                                   "intent-" + generateName());
    }
    public static RelyingPartyAccessCertificate generateAccessCertificate() {
        try {
            return new RelyingPartyAccessCertificate(
                CertificatesGenerator.generateX509Certificate());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to generate RelyingPartyAccessCertificate", e);
        }
    }
    public static List<RelyingPartyAccessCertificate> generateAccessCertificates() {
        return generateListBy(rng.nextInt(2, 4), EntityGenerator::generateAccessCertificate);
    }
}

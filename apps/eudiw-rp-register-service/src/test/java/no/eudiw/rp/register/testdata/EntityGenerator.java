package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.AccessCertificate;
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
            List.of(new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/Service_Provider"), new RelyingPartyEntitlement("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")),
            generateListBy(EntityGenerator::generateEaa),
            // NOTE: certificates are very expensive to generate, so no
            // certificates by default.
            new ArrayList<>());
    }

    public static RelyingParty generateRelyingPartyWithCertificates() {
        RelyingParty relyingParty = generateRelyingPartyNoId();
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

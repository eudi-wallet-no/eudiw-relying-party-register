package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.WalletRelyingPartyService;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEaa;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityGenerator extends TestDataGenerator {

    public static List<WalletRelyingParty> generateWalletRelyingParties(int numWalletRelyingParties) {
        Map<String, WalletRelyingParty> distinctWalletRelyingParties = new HashMap<>();
        while (distinctWalletRelyingParties.size() < numWalletRelyingParties) {
            WalletRelyingParty le = EntityGenerator.generateWalletRelyingParty();
            distinctWalletRelyingParties.put(le.getOrgno(), le);
        }
        return new ArrayList<>(distinctWalletRelyingParties.values());
    }

    public static WalletRelyingParty generateWalletRelyingParty() {
        return generateWalletRelyingParty(rng.nextInt(1, 6));
    }

    public static WalletRelyingParty generateWalletRelyingParty(int numInstances) {
        return new WalletRelyingParty(
            generateName(),
            generateValidOrgno(),
            true,
            generateListBy(numInstances, () ->
                generateRelyingPartyWithoutWalletRelyingParty().getWalletRelyingPartyService()));
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

    public static RelyingPartyInstance generateRelyingPartyWithoutWalletRelyingParty() {
        return generateRelyingPartyInstance(
            generateName(),
            sampleRelyingPartyEntitlements(),
            generateListBy(EntityGenerator::generateEaa),
            // NOTE: certificates are very expensive to generate, so no
            // certificates by default.
            new ArrayList<>()
        );
    }

    public static RelyingPartyInstance generateRelyingParty() {
        RelyingPartyInstance relyingPartyInstance = generateRelyingPartyWithoutWalletRelyingParty();
        WalletRelyingParty walletRelyingPartyWithZeroInstances = generateWalletRelyingParty(0);
        WalletRelyingPartyService service = relyingPartyInstance.getWalletRelyingPartyService();
        walletRelyingPartyWithZeroInstances.setServices(List.of(service));
        return relyingPartyInstance;
    }

    public static RelyingPartyInstance generateRelyingPartyInstance(
        String tradeName,
        List<RelyingPartyEntitlement> entitlements,
        List<RelyingPartyEaa> eaas,
        List<AccessCertificate> certificates
    ) {
        RelyingPartyInstance instance = new RelyingPartyInstance(entitlements, eaas, certificates);
        new WalletRelyingPartyService(tradeName, null, List.of(instance));
        return instance;
    }

    public static List<RelyingPartyInstance> instances(WalletRelyingParty walletRelyingParty) {
        return walletRelyingParty.getServices().stream()
            .flatMap(service -> service.getRelyingPartyInstances().stream())
            .toList();
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

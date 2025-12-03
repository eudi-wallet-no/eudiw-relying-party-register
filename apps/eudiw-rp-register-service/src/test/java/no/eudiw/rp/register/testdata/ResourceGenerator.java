package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResourceGenerator extends TestDataGenerator {

    public static List<RelyingPartyEntitlementResource> sampleRelyingPartyEntitlementResources() {
        return sampleRelyingPartyEntitlementResources(rng.nextInt(1, 4));
    }
    public static List<RelyingPartyEntitlementResource> sampleRelyingPartyEntitlementResources(int n) {
        return sampleEntitlements(n)
                   .stream()
                   .map(e -> new RelyingPartyEntitlementResource(e, e, null, List.of(generateCertificateResource())))
                   .toList();
    }

    public static RelyingPartyCertificateResource generateCertificateResource() {
        return new RelyingPartyCertificateResource(
            CertificatesGenerator.generateX509Certificate(), UUID.randomUUID());
    }

    public static CreateRelyingPartyResource generateCreateRelyingPartyResource() {
        return new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            sampleRelyingPartyEntitlementResources(),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource)
        );
    }

    public static EditRelyingPartyResource generateEditRelyingPartyResource() {
        return new EditRelyingPartyResource(
            generateName(),
            sampleRelyingPartyEntitlementResources(),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            true
       );
    }

    public static RelyingPartyEaaResource generateRelyingPartyEaaResource() {
        return new RelyingPartyEaaResource(
            generateRandomString(5, 20),
            generateRandomString(5, 20)
        );
    }

    public static RelyingPartyResource generateRelyingPartyResource() {
        long timeNow = Instant.now().toEpochMilli();

        return new RelyingPartyResource(
            UUID.randomUUID(),
            generateValidOrgno(),
            generateName(),
            generateName(),
            generatePublicSector(),
            sampleRelyingPartyEntitlementResources(),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            new ArrayList<>(),
            timeNow,
            timeNow,
            true
        );
    }

    public static RelyingPartyCsrResource generateRegisterRelyingPartyCsrResource() {
        return new RelyingPartyCsrResource(CertificatesGenerator.generatePKCS10Csr());
    }

    public static RelyingPartyCertificateResource generateRelyingPartyCertificateResource() {
        return new RelyingPartyCertificateResource(
            CertificatesGenerator.generateX509Certificate(), UUID.randomUUID());
    }
}

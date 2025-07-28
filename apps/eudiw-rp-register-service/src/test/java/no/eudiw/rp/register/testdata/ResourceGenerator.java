package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.eudiw.rp.register.data.entitlement.Entitlement;

import java.time.Instant;
import java.util.UUID;

public class ResourceGenerator extends TestDataGenerator {

    public static CreateRelyingPartyResource generateCreateRelyingPartyResource() {
        return new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            generatePublicSector(),
            sampleEntitlements().stream().map(Entitlement::toResource).toList(),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource)
        );
    }

    public static EditRelyingPartyResource generateEditRelyingPartyResource() {
        return new EditRelyingPartyResource(
            generateName(),
            generatePublicSector(),
            sampleEntitlements().stream().map(Entitlement::toResource).toList(),
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
            generatePublicSector(),
            sampleEntitlements().stream().map(Entitlement::toResource).toList(),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            timeNow,
            timeNow,
            true
        );
    }

    public static RelyingPartyCsrResource generateRegisterRelyingPartyCsrResource()
        throws Exception {
        return new RelyingPartyCsrResource(CertificatesGenerator.generatePKCS10Csr());
    }

    public static RelyingPartyAccessCertificateResource generateRelyingPartyAccessCertificateResource()
        throws Exception {
        return new RelyingPartyAccessCertificateResource(
            CertificatesGenerator.generateX509Certificate(), UUID.randomUUID());
    }
}

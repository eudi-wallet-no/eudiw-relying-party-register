package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.api.resource.accesscertificates.RegisterRelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.accesscertificates.RelyingPartyAccessCertificateResource;

import java.time.Instant;
import java.util.UUID;

public class ResourceGenerator extends TestDataGenerator {


    public static CreateRelyingPartyResource generateCreateRelyingPartyResource() {
        return new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            generatePublicSector(),
            generateListBy(ResourceGenerator::generateRelyingPartyEntitlementResource),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource)
        );
    }

    public static EditRelyingPartyResource generateEditRelyingPartyResource() {
        return new EditRelyingPartyResource(
            generateName(),
            generatePublicSector(),
            generateListBy(ResourceGenerator::generateRelyingPartyEntitlementResource),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            true
       );
    }

    public static RelyingPartyEntitlementResource generateRelyingPartyEntitlementResource() {
        return new RelyingPartyEntitlementResource(
            generateRandomString(5, 20)
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
            generateListBy(ResourceGenerator::generateRelyingPartyEntitlementResource),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            timeNow,
            timeNow,
            true
        );
    }

    public static RegisterRelyingPartyCsrResource generateRegisterRelyingPartyCsrResource()
        throws Exception {
        return new RegisterRelyingPartyCsrResource(CertificatesGenerator.generatePKCS10Csr());
    }

    public static RelyingPartyAccessCertificateResource generateRelyingPartyAccessCertificateResource()
        throws Exception {
        return new RelyingPartyAccessCertificateResource(CertificatesGenerator.generateX509Certificate());
    }
}

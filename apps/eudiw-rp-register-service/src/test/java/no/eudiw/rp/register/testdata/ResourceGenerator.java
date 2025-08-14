package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.api.resource.*;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ResourceGenerator extends TestDataGenerator {

    public static CreateRelyingPartyResource generateCreateRelyingPartyResource() {
        return new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            generatePublicSector(),
            List.of(new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/Service_Provider"), new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource)
        );
    }

    public static EditRelyingPartyResource generateEditRelyingPartyResource() {
        return new EditRelyingPartyResource(
            generateName(),
            generatePublicSector(),
            List.of(new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/Service_Provider"), new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")),
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
            List.of(new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/Service_Provider"), new RelyingPartyEntitlementResource("https://uri.etsi.org/19475/Entitlement/QEAA_Provider")),
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

    public static RelyingPartyCertificateResource generateRelyingPartyCertificateResource()
        throws Exception {
        return new RelyingPartyCertificateResource(
            CertificatesGenerator.generateX509Certificate(), UUID.randomUUID());
    }
}

package no.idporten.eudiw.rp.admin.testdata;

import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyAccessCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.accesscertificates.RelyingPartyCsrResource;

import java.time.Instant;
import java.util.*;

public class ResourceGenerator extends TestDataGenerator {

    private static final List<String> EXAMPLE_ENTITLEMENTS = new ArrayList<>(List.of(
        // NOTE: the actual set of entitlements may change, but this is not important
        // for the purposes of testing.
        "https://uri.etsi.org/19475/Entitlement/Service_Provider",
        "https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
        "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider",
        "https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider",
        "https://uri.etsi.org/19475/Entitlement/PID_Provider"
    ));

    public static List<RelyingPartyEntitlementResource> sampleRelyingPartyEntitlementResources() {
        Collections.shuffle(EXAMPLE_ENTITLEMENTS);
        return EXAMPLE_ENTITLEMENTS.subList(0, rng.nextInt(1, 4))
                                   .stream()
                                   .map(RelyingPartyEntitlementResource::new)
                                   .toList();
    }

    public static RelyingPartyEntitlementResource generateRelyingPartyEntitlementResource() {
        return new RelyingPartyEntitlementResource(
            EXAMPLE_ENTITLEMENTS.get(rng.nextInt(0, EXAMPLE_ENTITLEMENTS.size())));
    }

    public static RelyingPartyEaaResource generateRelyingPartyEaaResource() {
        return new RelyingPartyEaaResource(
            generateRandomString(5, 20),
            generateRandomString(5, 20)
        );
    }

    public static CreateRelyingPartyResource generateCreateRelyingPartyResource() {
        return new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            generateBoolean(),
            sampleRelyingPartyEntitlementResources(),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource)
        );
    }

    public static RelyingPartyResource generateRelyingPartyResource() {
        CreateRelyingPartyResource createResource = generateCreateRelyingPartyResource();
        long timeNow = Instant.now().toEpochMilli();
        return new RelyingPartyResource(
            UUID.randomUUID(),
            createResource.orgno(),
            createResource.name(),
            createResource.publicSector(),
            createResource.relyingPartyEntitlements(),
            createResource.relyingPartyEaas(),
            timeNow,
            timeNow,
            true
        );
    }
    public static List<RelyingPartyResource> generateRelyingPartiesResource() {
        return generateListBy(ResourceGenerator::generateRelyingPartyResource);
    }

    public static SearchForm generateSearchForm() {
        return new SearchForm(generateName(), generateBoolean(), new ArrayList<>(), 0, 25);
    }

    public static RelyingPartyAccessCertificateResource generateCertificateResource()
        throws Exception {
        return new RelyingPartyAccessCertificateResource(
            CertificatesGenerator.generateX509Certificate(), UUID.randomUUID());
    }

    public static RelyingPartyCsrResource generateCsrResource() throws Exception {
        return new RelyingPartyCsrResource(CertificatesGenerator.generatePKCS10Csr());
    }

    public static PagedResponse<RelyingPartyResource> generatePageResponse(List<RelyingPartyResource> content) {
        return new PagedResponse<>(
            content,
            new PagedResponse.PageMetadata(
                content.size(), 0, content.size(), 1
            )
        );
        /*return new PageResponse<>(
            content,
            null,
            true,
            (long) content.size(),
            1,
            content.size(),
            1,
            true,
            content.size(),
            null,
            false
        );*/
    }
}

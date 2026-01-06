package no.idporten.eudiw.rp.admin.testdata;

import no.idporten.eudiw.rp.admin.entitlements.Entitlements;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;

import java.time.Instant;
import java.util.*;

public class ResourceGenerator extends TestDataGenerator {

    public static List<RelyingPartyEntitlementResource> sampleRelyingPartyEntitlementResources() {
        return sampleEntitlements(rng.nextInt(1, 4))
            .stream()
            .map(e -> {
                boolean hasIssuerUrl = Entitlements.isIssuerEntitlement(e) && rng.nextFloat() >= 0.3;
                return new RelyingPartyEntitlementResource(e, e, hasIssuerUrl ? generateIssuerUrl() : null);
            })
            .toList();
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
            createResource.tradeName(),
            "Legal-entity-" + createResource.orgno(),
            rng.nextBoolean(),
            createResource.relyingPartyEntitlements(),
            createResource.relyingPartyEaas(),
            List.of(),
            List.of(),
            timeNow,
            timeNow,
            true
        );
    }
    public static List<RelyingPartyResource> generateRelyingPartiesResource() {
        return generateListBy(ResourceGenerator::generateRelyingPartyResource);
    }

    public static SearchForm generateSearchForm() {
        return new SearchForm(generateName(), generateBoolean(), new ArrayList<>());
    }

    public static RelyingPartyCertificateResource generateCertificateResource() {
        String entitlement = sampleEntitlements(1).getFirst();
        boolean isIssuerEntitlement = Entitlements.isIssuerEntitlement(entitlement);
        return new RelyingPartyCertificateResource(
            CertificatesGenerator.generateX509Certificate(),
            UUID.randomUUID(),
            isIssuerEntitlement ? entitlement : null);
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
    }
}

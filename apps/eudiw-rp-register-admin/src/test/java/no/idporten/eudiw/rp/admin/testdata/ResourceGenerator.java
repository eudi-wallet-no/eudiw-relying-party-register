package no.idporten.eudiw.rp.admin.testdata;

import no.idporten.eudiw.rp.admin.entitlements.Entitlements;
import no.idporten.eudiw.rp.admin.web.form.SearchForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCsrResource;

import java.time.Instant;
import java.util.*;

public class ResourceGenerator extends TestDataGenerator {

    // private static final String CREDENTIAL_ISSUER_URL = "https://utsteder.test.eidas2sandkasse.net/.well-known/openid-credential-issuer";
    // NOTE: null for the time being, since credential issuer URLs have not been
    // added to create/edit forms yet.
    private static final String CREDENTIAL_ISSUER_URL = null;

    public static List<RelyingPartyEntitlementResource> sampleRelyingPartyEntitlementResources() {
        return sampleEntitlements(rng.nextInt(1, 4))
            .stream()
            .map(e -> new RelyingPartyEntitlementResource(e, e, CREDENTIAL_ISSUER_URL))
            .toList();
    }

    public static RelyingPartyEntitlementResource generateRelyingPartyEntitlementResource() {
        String e = EXAMPLE_ENTITLEMENTS.get(rng.nextInt(0, EXAMPLE_ENTITLEMENTS.size()));
        return new RelyingPartyEntitlementResource(e);
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

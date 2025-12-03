package no.idporten.eudiw.rp.register.lookup.testdata;

import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.*;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialMetadata;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.CredentialsResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.credentials.Display;

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


    public static RelyingPartyResource generateRelyingPartyResource() {
        long timeNow = Instant.now().toEpochMilli();
        return new RelyingPartyResource(
            UUID.randomUUID(),
            generateValidOrgno(),
            generateName(),
            generateBoolean(),
            generateListBy(ResourceGenerator::generateRelyingPartyEntitlementResource),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            timeNow,
            timeNow
        );
    }
    public static List<RelyingPartyResource> generateRelyingPartiesResource() {
        return generateListBy(ResourceGenerator::generateRelyingPartyResource);
    }

    public static SearchForm generateSearchForm() {
        SearchForm searchForm = new SearchForm();
        searchForm.setSearchTerm(generateName());
        return searchForm;
    }

    public static PagedResponse<RelyingPartyResource> generatePageResponse(List<RelyingPartyResource> content) {
        return new PagedResponse<>(
            content,
            new PagedResponse.PageMetadata(
                content.size(), 0, content.size(), 1
            )
        );
    }

    private static final List<String> localeStrs = new ArrayList<>(List.of("no", "en", "da"));

    private static List<Display> generateDisplays() {
        Collections.shuffle(localeStrs);
        int n = rng.nextInt(1, localeStrs.size());
        // description is optional, so leave it out for some displays
        String optionalDescription = rng.nextFloat() >= 0.6 ? generateName() : null;
        return localeStrs.stream()
                         .limit(n)
                         .map(s -> new Display(generateName(), s, optionalDescription))
                         .toList();
    }

    private static CredentialMetadata.Claims generateClaims() {
        int n = rng.nextInt(1, 3);
        var paths = generateListBy(n, TestDataGenerator::generateName);
        var displays = generateDisplays();
        return new CredentialMetadata.Claims(paths, displays);
    }

    private static CredentialMetadata generateMetadata() {
        var displays = generateDisplays();
        var claims = generateListBy(rng.nextInt(3, 10), ResourceGenerator::generateClaims);
        return new CredentialMetadata(displays, claims);
    }

    public static CredentialResource generateCredentialResource() {
        String format = rng.nextBoolean() ? "mso_mdoc" : "dc+sd-jwt";
        String issuer = generateName();
        String configurationId = generateName();
        String credentialType = generateName();
        CredentialMetadata metadata = generateMetadata();
        List<Display> issuerDisplays = generateDisplays();
        return new CredentialResource(format, issuer, issuerDisplays, configurationId, credentialType, metadata);
    }

    public static CredentialsResource generateCredentialsResource() {
        return new CredentialsResource(
            generateListBy(rng.nextInt(1, 5),
                           ResourceGenerator::generateCredentialResource));
    }
}

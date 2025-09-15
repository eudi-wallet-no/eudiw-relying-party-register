package no.idporten.eudiw.rp.register.lookup.testdata;

import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
        return new SearchForm(generateName(), new ArrayList<>());
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

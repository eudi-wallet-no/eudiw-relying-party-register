package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.api.resource.CreateRelyingPartyResource;
import no.eudiw.rp.register.api.resource.RelyingPartyEaaResource;
import no.eudiw.rp.register.api.resource.RelyingPartyEntitlementResource;

import java.util.List;

public class ResourceGenerator extends TestDataGenerator {

    public static CreateRelyingPartyResource generateCreateRelyingPartyResource() {
        return new CreateRelyingPartyResource(
            generateOrgno(),
            generateName(),
            generatePublicSector(),
            List.of(generateRelyingPartyEntitlementResource()),
            List.of(generateRelyingPartyEaaResource())
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
}

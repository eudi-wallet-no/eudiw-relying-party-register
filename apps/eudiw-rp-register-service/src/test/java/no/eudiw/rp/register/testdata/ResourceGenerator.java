package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.api.resource.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ResourceGenerator extends TestDataGenerator {

    public static CreateRelyingPartyResource generateCreateRelyingPartyResource() {
        return new CreateRelyingPartyResource(
            generateValidOrgno(),
            generateName(),
            generatePublicSector(),
            List.of(generateRelyingPartyEntitlementResource()),
            List.of(generateRelyingPartyEaaResource())
        );
    }

    public static EditRelyingPartyResource generateEditRelyingPartyResource() {
        return new EditRelyingPartyResource(
            generateName(),
            generatePublicSector(),
            List.of(generateRelyingPartyEntitlementResource()),
            List.of(generateRelyingPartyEaaResource()),
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
        Instant timeNow = Instant.now();

        return new RelyingPartyResource(
            UUID.randomUUID(),
            generateValidOrgno(),
            generateName(),
            generatePublicSector(),
            List.of(generateRelyingPartyEntitlementResource()),
            List.of(generateRelyingPartyEaaResource()),
            timeNow.toEpochMilli(),
            timeNow.toEpochMilli(),
            true
        );
    }
}

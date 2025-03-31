package no.idporten.eudiw.rp.register.lookup.testdata;

import no.idporten.eudiw.rp.register.lookup.web.resource.*;

import java.time.Instant;
import java.util.UUID;

public class ResourceGenerator extends TestDataGenerator {

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
            generateBoolean(),
            generateListBy(ResourceGenerator::generateRelyingPartyEntitlementResource),
            generateListBy(ResourceGenerator::generateRelyingPartyEaaResource),
            timeNow,
            timeNow,
            true
        );
    }
    public static RelyingPartiesResource generateRelyingPartiesResource() {
        return new RelyingPartiesResource(
            generateListBy(ResourceGenerator::generateRelyingPartyResource));
    }

    public static SearchForm generateSearchForm() {
        return new SearchForm(
            generateValidOrgno(),
            generateBoolean() ? SearchForm.SearchSector.PUBLIC : SearchForm.SearchSector.PRIVATE,
            generateBoolean());
    }
}

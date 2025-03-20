package no.eudiw.rp.register.testdata;

import no.eudiw.rp.register.data.entity.RelyingParty;

public class EntityGenerator extends TestDataGenerator {

    public static RelyingParty generateRelyingPartyNoId() {
        return new RelyingParty(generateName(),
                                generateValidOrgno(),
                                generatePublicSector());
    }
}

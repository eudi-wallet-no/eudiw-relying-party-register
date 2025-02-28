package no.eudiw.rp.register;

import no.eudiw.rp.register.data.entity.RelyingParty;
import no.eudiw.rp.register.data.entity.RelyingPartyEntitlement;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;

@Component
public class TestDataGenerator {

    private static final SecureRandom rng = new SecureRandom();

    private static String generateRandomString(int minLength, int maxLength) {
        StringBuilder out = new StringBuilder(rng.nextInt(minLength, maxLength));
        for (int i = 0; i < out.capacity(); i++)
            out.append((char) rng.nextInt('a', 'z'));
        return out.toString();
    }

    public static UUID generateUUID() {
        return UUID.randomUUID();
    }

    public static String generateOrgno() {
        String tmp = String.format("%09d", rng.nextInt());
        return tmp.substring(tmp.length() - 9);
    }

    public static String generateName() {
        return generateRandomString(5, 15);
    }

    public static Boolean generatePublicSector() {
        return rng.nextInt() % 2 == 0;
    }

    public static RelyingPartyEntitlement generateRelyingPartyEntitlement() {
        return new RelyingPartyEntitlement(generateRandomString(10, 20));
    }

    public static RelyingParty generateRelyingPartyNoId() {
        return new RelyingParty(generateName(),
                                generateOrgno(),
                                generatePublicSector());
    }
}

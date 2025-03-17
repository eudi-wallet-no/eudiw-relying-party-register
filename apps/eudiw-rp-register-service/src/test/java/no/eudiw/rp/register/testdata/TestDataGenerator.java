package no.eudiw.rp.register.testdata;

import java.security.SecureRandom;

public class TestDataGenerator {

    protected static final SecureRandom rng = new SecureRandom();

    protected static String generateRandomString(int minLength, int maxLength) {
        StringBuilder out = new StringBuilder(rng.nextInt(minLength, maxLength));
        for (int i = 0; i < out.capacity(); i++)
            out.append((char) rng.nextInt('a', 'z'));
        return out.toString();
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

}

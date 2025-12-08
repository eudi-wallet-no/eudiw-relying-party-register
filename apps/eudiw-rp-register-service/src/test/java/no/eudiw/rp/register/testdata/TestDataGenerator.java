package no.eudiw.rp.register.testdata;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestDataGenerator {

    protected static final SecureRandom rng = new SecureRandom();

    protected static String generateRandomString(int minLength, int maxLength) {
        StringBuilder out = new StringBuilder(rng.nextInt(minLength, maxLength));
        for (int i = 0; i < out.capacity(); i++)
            out.append((char) rng.nextInt('a', 'z'));
        return out.toString();
    }

    public static String generateName() {
        return generateRandomString(5, 15);
    }

    public static Boolean generatePublicSector() {
        return rng.nextInt() % 2 == 0;
    }

    protected static <T> List<T> generateListBy(Supplier<T> supplier) {
        return generateListBy(rng.nextInt(2, 11), supplier);
    }
    protected static <T> List<T> generateListBy(int n, Supplier<T> supplier) {
        return IntStream.range(0, n)
                        .mapToObj(_ -> supplier.get())
                        .collect(Collectors.toList());
    }

    // orgno weights, from least to most significant digit.
    private static final int[] ORGNO_WEIGHTS = {2, 3, 4, 5, 6, 7, 2, 3};
    public static final int ORGNO_LENGTH = 9;

    private static int getControlDigit(int orgno) {
        int sumOfProducts = 0;
        for (int i = 0; i < 8; i++) {
            orgno /= 10;
            int currentOrgnoDigit = orgno % 10;
            sumOfProducts += currentOrgnoDigit * ORGNO_WEIGHTS[i];
        }
        int remainder = sumOfProducts % 11;
        return remainder != 0 ? 11 - remainder : 0;
    }


    public static String generateValidOrgno() {
        int rawOrgno = rng.nextInt(800000000, 999999999 + 1); // generate orgnos starting with 8 and 9

        int controlDigit = getControlDigit(rawOrgno);
        if (controlDigit == 10) {
            // make orgno valid by adding 1 to least significant non-control digit.
            // works because a weighted orgno digit can never have 11 as a factor
            // (given current orgno weights).
            int leastSignificantNonControlDigit = rawOrgno / 10 % 10;
            int newLeastSignificantNonControlDigit = (leastSignificantNonControlDigit + 1) % 10;
            rawOrgno = rawOrgno - rawOrgno % 100 + newLeastSignificantNonControlDigit * 10;
            controlDigit = getControlDigit(rawOrgno);
        }
        int orgno = rawOrgno - rawOrgno % 10 + controlDigit;
        return Integer.toString(orgno);
    }

    public static String generateInvalidOrgno() {
        // guarantee an invalid orgno by modifying a valid one.
        StringBuilder buf = new StringBuilder(generateValidOrgno());

        int modifyIndex = rng.nextInt(0, ORGNO_LENGTH);
        int digit = Character.digit(buf.charAt(modifyIndex), 10);
        int invalidDigit = (digit + 1) % 10;

        buf.setCharAt(modifyIndex, Character.forDigit(invalidDigit, 1));
        return buf.toString();
    }

    private static final List<String> EXAMPLE_ENTITLEMENTS = new ArrayList<>(List.of(
        // NOTE: the actual set of entitlements may change, but this is not important
        // for the purposes of testing.
        "https://uri.etsi.org/19475/Entitlement/Service_Provider",
        "https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
        "https://uri.etsi.org/19475/Entitlement/Non_Q_EAA_Provider",
        "https://uri.etsi.org/19475/Entitlement/PUB_EAA_Provider",
        "https://uri.etsi.org/19475/Entitlement/PID_Provider"
    ));

    protected static List<String> sampleEntitlements(int n) {
        if (n > EXAMPLE_ENTITLEMENTS.size()) {
            throw new RuntimeException("Not enough sample entitlements (requested %s, max %s)"
                                           .formatted(n, EXAMPLE_ENTITLEMENTS.size()));
        }
        Collections.shuffle(EXAMPLE_ENTITLEMENTS);
        return EXAMPLE_ENTITLEMENTS.subList(0, n);
    }
}

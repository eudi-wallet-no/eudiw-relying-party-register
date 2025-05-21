package no.idporten.eudiw.rp.admin.testdata;

import no.idporten.eudiw.rp.admin.web.resource.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ResourceGenerator {

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
        return new SearchForm(generateName(), generateBoolean());
    }

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

    public static boolean generateBoolean() {
        return rng.nextBoolean();
    }

    protected static <T> List<T> generateListBy(Supplier<T> supplier) {
        return IntStream.range(0, rng.nextInt(1, 10))
                .mapToObj(_ -> supplier.get())
                .toList();
    }

    private static final int[] ORGNO_WEIGHTS = {3, 2, 7, 6, 5, 4, 3, 2, 0};
    public static final int ORGNO_LENGTH = 9;

    private static int getControlDigit(int[] orgnoDigits) {
        int sumOfProducts = 0;
        for (int i = 0; i < ORGNO_LENGTH - 1; i++)
            sumOfProducts += orgnoDigits[i] * ORGNO_WEIGHTS[i];
        int remainder = sumOfProducts % 11;
        return remainder != 0 ? 11 - remainder : 0;
    }

    public static String generateValidOrgno() {
        int[] randDigits = new int[ORGNO_LENGTH];
        for (int i = 0; i < randDigits.length; i++)
            randDigits[i] = rng.nextInt(0, 9);

        int controlDigit = getControlDigit(randDigits);
        if (controlDigit == 10) {
            // make control digit valid by adding 1 to a random index. works
            // because a weighted orgno digit can never have 11 as a factor (given
            // current orgno weights).
            int modifyIndex = rng.nextInt(0, ORGNO_LENGTH - 1);
            randDigits[modifyIndex] = (randDigits[modifyIndex] + 1) % 10;
            controlDigit = getControlDigit(randDigits);
        }

        randDigits[ORGNO_LENGTH - 1] = controlDigit;
        return Arrays.stream(randDigits)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());
    }
}
